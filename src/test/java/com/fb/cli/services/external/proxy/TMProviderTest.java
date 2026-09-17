package com.fb.cli.services.external.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.ProviderStrategy;
import com.fb.cli.enums.RotationType;
import com.fb.cli.services.external.SendRequest;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TMProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProxyResponseMapper responseMapper = new ProxyResponseMapper(objectMapper);

    @Test
    void shouldGetNewProxyUsingDynamicEntityConfig() {
        String json = """
                {
                    "code": 0,
                    "message": "OK",
                    "data": {
                        "ip": "103.15.22.1",
                        "port": 8080,
                        "auth_user": "u1",
                        "auth_pass": "p1",
                        "scheme": "http",
                        "timeout": 300
                    }
                }
                """;

        String configJson = """
                {
                    "code": {
                        "SUCCESS": 0,
                        "API_KEY_COOLDOWN": 5,
                        "API_KEY_EXPIRED": 24
                    },
                    "endpoints": {
                        "GET_NEW_PROXY": {
                            "endpoint_name": "GET_NEW_PROXY",
                            "method": "POST",
                            "path": "/api/v1/get-new-ip",
                            "response_mapping": {
                                "root_path": "data",
                                "host": "ip",
                                "port": "port",
                                "username": "auth_user",
                                "password": "auth_pass",
                                "protocol": "scheme"
                            }
                        }
                    }
                }
                """;

        ProxyProvider provider = ProxyProvider.builder()
                .name("TMProxy")
                .baseUrl("https://tmproxy.com/api/proxy")
                .strategy(ProviderStrategy.TM_Strategy)
                .proxyType(RotationType.ROTATING)
                .configJson(configJson)
                .build();

        OkHttpClient client = createMockClient(json);
        SendRequest sendRequest = new SendRequest(client);
        TMProvider tmProvider = new TMProvider(provider, sendRequest, responseMapper);

        ProxyInfo proxy = tmProvider.getNewProxy("test_api_key");

        assertNotNull(proxy);
        assertEquals("103.15.22.1", proxy.host());
        assertEquals(8080, proxy.port());
        assertEquals("u1", proxy.username());
        assertEquals("p1", proxy.password());
        assertEquals("http", proxy.protocol());
    }

    @Test
    void shouldFallbackToCurrentProxyWhenCooldownCodeMatchesEntityConfig() {
        String configJson = """
                {
                    "code": {
                        "SUCCESS": 0,
                        "API_KEY_COOLDOWN": 5
                    },
                    "endpoints": {
                        "GET_NEW_PROXY": {
                            "path": "/get-new-proxy"
                        },
                        "GET_CURRENT_PROXY": {
                            "path": "/get-current-proxy"
                        }
                    }
                }
                """;

        ProxyProvider provider = ProxyProvider.builder()
                .name("TMProxy")
                .baseUrl("https://tmproxy.com/api/proxy")
                .strategy(ProviderStrategy.TM_Strategy)
                .proxyType(RotationType.ROTATING)
                .configJson(configJson)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String path = chain.request().url().encodedPath();
                    String responseJson;
                    if (path.contains("get-new-proxy")) {
                        responseJson = """
                                {
                                    "code": 5,
                                    "message": "Chua den luot doi IP",
                                    "data": null
                                }
                                """;
                    } else {
                        responseJson = """
                                {
                                    "code": 0,
                                    "message": "",
                                    "data": {
                                        "https": "103.99.88.77:9999",
                                        "timeout": 120
                                    }
                                }
                                """;
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(responseJson, MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(client);
        TMProvider tmProvider = new TMProvider(provider, sendRequest, responseMapper);

        ProxyInfo proxy = tmProvider.getNewProxy("test_api_key");

        assertNotNull(proxy);
        assertEquals("103.99.88.77", proxy.host());
        assertEquals(9999, proxy.port());
    }

    @Test
    void shouldGetKeyExpiredTimeUsingDynamicEndpointConfig() {
        String json = """
                {
                    "code": 0,
                    "message": "string",
                    "data": {
                        "expired_at": "2025-01-15T07:35:03.473Z"
                    }
                }
                """;

        String configJson = """
                {
                    "code": {
                        "SUCCESS": 0
                    },
                    "endpoints": {
                        "KEY_EXPIRED_TIME": {
                            "path": "/api/proxy/stats"
                        }
                    }
                }
                """;

        ProxyProvider provider = ProxyProvider.builder()
                .name("TMProxy")
                .baseUrl("https://tmproxy.com")
                .strategy(ProviderStrategy.TM_Strategy)
                .proxyType(RotationType.ROTATING)
                .configJson(configJson)
                .build();

        OkHttpClient client = createMockClient(json);
        SendRequest sendRequest = new SendRequest(client);
        TMProvider tmProvider = new TMProvider(provider, sendRequest, responseMapper);

        LocalDateTime expiredTime = tmProvider.keyExpiredTime("test_api_key");

        assertNotNull(expiredTime);
        assertEquals(2025, expiredTime.getYear());
        assertEquals(1, expiredTime.getMonthValue());
        assertEquals(15, expiredTime.getDayOfMonth());
        assertEquals(7, expiredTime.getHour());
        assertEquals(35, expiredTime.getMinute());
        assertEquals(3, expiredTime.getSecond());
    }

    private OkHttpClient createMockClient(String responseJson) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(responseJson, MediaType.get("application/json")))
                        .build())
                .build();
    }
}
