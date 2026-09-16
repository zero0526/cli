package com.fb.cli.configs;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OkHttpConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OkHttpConfig.class);

    @Test
    void shouldLoadDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OkHttpProperties.class);
            assertThat(context).hasSingleBean(ConnectionPool.class);
            assertThat(context).hasSingleBean(OkHttpClient.class);

            OkHttpProperties properties = context.getBean(OkHttpProperties.class);
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.getWriteTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.getCallTimeout()).isEqualTo(Duration.ZERO);
            assertThat(properties.getPingInterval()).isEqualTo(Duration.ZERO);
            assertThat(properties.isRetryOnConnectionFailure()).isTrue();
            assertThat(properties.isFollowRedirects()).isTrue();
            assertThat(properties.isFollowSslRedirects()).isTrue();
            assertThat(properties.getConnectionPool().getMaxIdleConnections()).isEqualTo(10);
            assertThat(properties.getConnectionPool().getKeepAliveDuration()).isEqualTo(Duration.ofMinutes(5));

            OkHttpClient client = context.getBean(OkHttpClient.class);
            assertThat(client.connectTimeoutMillis()).isEqualTo(10000);
            assertThat(client.readTimeoutMillis()).isEqualTo(30000);
            assertThat(client.writeTimeoutMillis()).isEqualTo(30000);
            assertThat(client.callTimeoutMillis()).isEqualTo(0);
            assertThat(client.pingIntervalMillis()).isEqualTo(0);
            assertThat(client.retryOnConnectionFailure()).isTrue();
            assertThat(client.followRedirects()).isTrue();
            assertThat(client.followSslRedirects()).isTrue();
            assertThat(client.connectionPool()).isNotNull();
        });
    }

    @Test
    void shouldBindCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "okhttp.connect-timeout=5s",
                        "okhttp.read-timeout=15s",
                        "okhttp.write-timeout=20s",
                        "okhttp.call-timeout=60s",
                        "okhttp.ping-interval=10s",
                        "okhttp.retry-on-connection-failure=false",
                        "okhttp.follow-redirects=false",
                        "okhttp.follow-ssl-redirects=false",
                        "okhttp.connection-pool.max-idle-connections=25",
                        "okhttp.connection-pool.keep-alive-duration=10m"
                )
                .run(context -> {
                    OkHttpProperties properties = context.getBean(OkHttpProperties.class);
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(properties.getWriteTimeout()).isEqualTo(Duration.ofSeconds(20));
                    assertThat(properties.getCallTimeout()).isEqualTo(Duration.ofSeconds(60));
                    assertThat(properties.getPingInterval()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(properties.isRetryOnConnectionFailure()).isFalse();
                    assertThat(properties.isFollowRedirects()).isFalse();
                    assertThat(properties.isFollowSslRedirects()).isFalse();
                    assertThat(properties.getConnectionPool().getMaxIdleConnections()).isEqualTo(25);
                    assertThat(properties.getConnectionPool().getKeepAliveDuration()).isEqualTo(Duration.ofMinutes(10));

                    OkHttpClient client = context.getBean(OkHttpClient.class);
                    assertThat(client.connectTimeoutMillis()).isEqualTo(5000);
                    assertThat(client.readTimeoutMillis()).isEqualTo(15000);
                    assertThat(client.writeTimeoutMillis()).isEqualTo(20000);
                    assertThat(client.callTimeoutMillis()).isEqualTo(60000);
                    assertThat(client.pingIntervalMillis()).isEqualTo(10000);
                    assertThat(client.retryOnConnectionFailure()).isFalse();
                    assertThat(client.followRedirects()).isFalse();
                    assertThat(client.followSslRedirects()).isFalse();
                });
    }
}
