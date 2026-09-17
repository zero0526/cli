package com.fb.cli.services.external.interceptor;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.services.external.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@Component
public class ProxyInterceptor implements RequestInterceptor {

    private final ObjectProvider<ProxyManager> proxyManagerProvider;
    private ProxyManager directProxyManager;

    @Autowired
    public ProxyInterceptor(@Lazy ObjectProvider<ProxyManager> proxyManagerProvider) {
        this.proxyManagerProvider = proxyManagerProvider;
    }

    /**
     * Constructor dùng cho testing hoặc khởi tạo trực tiếp
     */
    public ProxyInterceptor(ProxyManager proxyManager) {
        this.proxyManagerProvider = null;
        this.directProxyManager = proxyManager;
    }

    private ProxyManager getProxyManager() {
        if (this.directProxyManager != null) {
            return this.directProxyManager;
        }
        if (this.proxyManagerProvider != null) {
            return this.proxyManagerProvider.getIfAvailable();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -100; // Ưu tiên chạy đầu tiên để resolve proxy sớm nhất
    }

    @Override
    public Request.Builder intercept(Request.Builder builder, RequestOptions options) {
        if (options == null) {
            return builder;
        }

        // Nếu đã có proxy được gán trước đó thì không cần resolve lại
        if (options.getProxy() != null) {
            return builder;
        }

        // Nếu có cấu hình sampler -> tự động gọi ProxyManager kéo proxy phù hợp
        if (options.getSampler() != null) {
            resolveProxy(options);
        }

        return builder;
    }

    /**
     * Kéo proxy từ ProxyManager dựa trên sampler trong options
     */
    public ProxyInfo resolveProxy(RequestOptions options) {
        if (options == null || options.getSampler() == null) {
            return null;
        }

        ProxyManager proxyManager = getProxyManager();
        if (proxyManager == null) {
            log.error("[ProxyInterceptor] ProxyManager chưa được khởi tạo hoặc không khả dụng");
            return null;
        }

        RandomSamplingCfg sampler = options.getSampler();
        ProxyInfo proxyInfo = proxyManager.getProxy(sampler);

        if (proxyInfo != null) {
            log.info("[ProxyInterceptor] Đã lấy proxy thành công cho request: {}:{} ({})",
                    proxyInfo.host(), proxyInfo.port(), proxyInfo.protocol());
            options.setProxy(proxyInfo);
        } else {
            log.error("[ProxyInterceptor] Không thể lấy proxy thỏa mãn sampler: {}", sampler);
        }

        return proxyInfo;
    }

    /**
     * Áp dụng cấu hình Proxy vào OkHttpClient
     */
    public OkHttpClient applyProxy(OkHttpClient baseClient, ProxyInfo proxyInfo) {
        if (baseClient == null || proxyInfo == null || proxyInfo.host() == null) {
            return baseClient;
        }

        Proxy.Type proxyType = "socks5".equalsIgnoreCase(proxyInfo.protocol())
                ? Proxy.Type.SOCKS
                : Proxy.Type.HTTP;

        Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyInfo.host(), proxyInfo.port()));
        OkHttpClient.Builder clientBuilder = baseClient.newBuilder().proxy(proxy);

        // Cấu hình Proxy Authenticator nếu proxy có username / password
        if (proxyInfo.username() != null && !proxyInfo.username().isBlank()) {
            clientBuilder.proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(
                        proxyInfo.username(),
                        proxyInfo.password() != null ? proxyInfo.password() : ""
                );
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            });
        }

        return clientBuilder.build();
    }
}
