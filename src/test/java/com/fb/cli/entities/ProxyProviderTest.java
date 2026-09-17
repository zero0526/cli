package com.fb.cli.entities;

import com.fb.cli.dtos.proxy.ProviderCodeConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProviderTest {

    @Test
    void shouldParseCodeMapFromConfigJson() {
        String configJson = """
                {
                    "code": {
                        "SUCCESS": 0,
                        "API_KEY_COOLDOWN": 5,
                        "API_KEY_EXPIRED": 24,
                        "CUSTOM_ERROR": 99
                    }
                }
                """;

        ProxyProvider provider = ProxyProvider.builder()
                .name("TMProxy")
                .configJson(configJson)
                .build();

        assertEquals(0, provider.getCode(ProviderCodeConstant.SUCCESS));
        assertEquals(5, provider.getCode(ProviderCodeConstant.API_KEY_COOLDOWN));
        assertEquals(24, provider.getCode(ProviderCodeConstant.API_KEY_EXPIRED));
        assertEquals(99, provider.getCode("CUSTOM_ERROR"));
        assertNull(provider.getCode("NON_EXISTING"));

        assertTrue(provider.isSuccess(0));
        assertFalse(provider.isSuccess(5));

        assertTrue(provider.isCooldown(5));
        assertFalse(provider.isCooldown(0));

        assertTrue(provider.isExpired(24));
        assertFalse(provider.isExpired(5));

        assertTrue(provider.isCode("CUSTOM_ERROR", 99));
    }
}
