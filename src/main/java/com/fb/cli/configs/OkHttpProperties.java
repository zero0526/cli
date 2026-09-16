package com.fb.cli.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for OkHttp client connections.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "okhttp")
public class OkHttpProperties {

    /**
     * Connect timeout for new connections.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout for web socket and HTTP requests.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Write timeout for web socket and HTTP requests.
     */
    private Duration writeTimeout = Duration.ofSeconds(30);

    /**
     * Complete call timeout (including name resolution, connect, write, and read).
     * Default is Duration.ZERO (no timeout).
     */
    private Duration callTimeout = Duration.ZERO;

    /**
     * Ping interval for HTTP/2 and WebSocket connections.
     * Default is Duration.ZERO (disabled).
     */
    private Duration pingInterval = Duration.ZERO;

    /**
     * Whether to retry when a connectivity problem is encountered.
     */
    private boolean retryOnConnectionFailure = true;

    /**
     * Whether to follow HTTP redirects (e.g. 301, 302).
     */
    private boolean followRedirects = true;

    /**
     * Whether to follow SSL/TLS redirects (HTTP to HTTPS or vice-versa).
     */
    private boolean followSslRedirects = true;

    /**
     * OkHttp connection pool settings.
     */
    private ConnectionPool connectionPool = new ConnectionPool();

    @Getter
    @Setter
    public static class ConnectionPool {

        /**
         * The maximum number of idle connections to keep in the pool.
         */
        private int maxIdleConnections = 10;

        /**
         * Duration to keep idle connections alive in the pool.
         */
        private Duration keepAliveDuration = Duration.ofMinutes(5);
    }
}
