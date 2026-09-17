package com.fb.cli.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.ResponseMappingConfig;
import com.fb.cli.services.external.proxy.ProxyResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProxyResponseMapperTest {

    private ProxyResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProxyResponseMapper(new ObjectMapper());
    }

    @Test
    void shouldMapStandardNestedJsonResponse() {
        String json = """
                {
                    "code": 0,
                    "data": {
                        "ip": "103.15.22.1",
                        "port": 8080,
                        "auth_user": "user1",
                        "auth_pass": "pass1",
                        "scheme": "socks5",
                        "expired_timestamp": 1750000000
                    }
                }
                """;

        ResponseMappingConfig config = ResponseMappingConfig.builder()
                .rootPath("data")
                .host("ip")
                .port("port")
                .username("auth_user")
                .password("auth_pass")
                .protocol("scheme")
                .expireAt("expired_timestamp")
                .build();

        ProxyInfo info = mapper.mapToProxyInfo(json, config);

        assertNotNull(info);
        assertEquals("103.15.22.1", info.host());
        assertEquals(8080, info.port());
        assertEquals("user1", info.username());
        assertEquals("pass1", info.password());
        assertEquals("socks5", info.protocol());
        assertEquals(1750000000L, info.expireAt());
    }

    @Test
    void shouldMapRawProxyColonFormat() {
        String json = """
                {
                    "status": "success",
                    "proxy": "192.168.1.1:9090:admin:secret",
                    "timeout": 1740000000
                }
                """;

        ResponseMappingConfig config = ResponseMappingConfig.builder()
                .rawProxy("proxy")
                .expireAt("timeout")
                .defaultProtocol("http")
                .build();

        ProxyInfo info = mapper.mapToProxyInfo(json, config);

        assertNotNull(info);
        assertEquals("192.168.1.1", info.host());
        assertEquals(9090, info.port());
        assertEquals("admin", info.username());
        assertEquals("secret", info.password());
        assertEquals("http", info.protocol());
        assertEquals(1740000000L, info.expireAt());
    }

    @Test
    void shouldMapRawProxyAtFormat() {
        String json = """
                {
                    "proxy_url": "user1:pass1@10.0.0.1:3128"
                }
                """;

        ResponseMappingConfig config = ResponseMappingConfig.builder()
                .rawProxy("proxy_url")
                .defaultProtocol("https")
                .build();

        ProxyInfo info = mapper.mapToProxyInfo(json, config);

        assertNotNull(info);
        assertEquals("10.0.0.1", info.host());
        assertEquals(3128, info.port());
        assertEquals("user1", info.username());
        assertEquals("pass1", info.password());
        assertEquals("https", info.protocol());
    }
}
