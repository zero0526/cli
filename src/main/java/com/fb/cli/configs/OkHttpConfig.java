package com.fb.cli.configs;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration for OkHttpClient and its ConnectionPool.
 */
@Configuration
@EnableConfigurationProperties(OkHttpProperties.class)
public class OkHttpConfig {

    @Bean
    @ConditionalOnMissingBean
    public ConnectionPool okHttpConnectionPool(OkHttpProperties properties) {
        OkHttpProperties.ConnectionPool pool = properties.getConnectionPool();
        return new ConnectionPool(
                pool.getMaxIdleConnections(),
                pool.getKeepAliveDuration().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(OkHttpProperties properties, ConnectionPool okHttpConnectionPool) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (properties.getConnectTimeout() != null) {
            builder.connectTimeout(properties.getConnectTimeout());
        }
        if (properties.getReadTimeout() != null) {
            builder.readTimeout(properties.getReadTimeout());
        }
        if (properties.getWriteTimeout() != null) {
            builder.writeTimeout(properties.getWriteTimeout());
        }
        if (properties.getCallTimeout() != null) {
            builder.callTimeout(properties.getCallTimeout());
        }
        if (properties.getPingInterval() != null) {
            builder.pingInterval(properties.getPingInterval());
        }

        builder.retryOnConnectionFailure(properties.isRetryOnConnectionFailure())
                .followRedirects(properties.isFollowRedirects())
                .followSslRedirects(properties.isFollowSslRedirects())
                .connectionPool(okHttpConnectionPool);

        return builder.build();
    }
}
