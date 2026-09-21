package com.fb.cli.domains.facebook.services;

import com.fb.cli.enums.BotStatus;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CheckLiveUIDTest {

    private SendRequest sendRequest;
    private CheckLiveUID checkLiveUID;

    @BeforeEach
    void setUp() {
        sendRequest = Mockito.mock(SendRequest.class);
        checkLiveUID = new CheckLiveUID(sendRequest);
    }

    @Test
    @DisplayName("Trả về ERROR khi UID rỗng hoặc null")
    void testCheckLive_BlankUID() {
        assertEquals(BotStatus.ERROR, checkLiveUID.checkLive(null));
        assertEquals(BotStatus.ERROR, checkLiveUID.checkLive("   "));
    }

    @Test
    @DisplayName("Trả về LIVE khi API phản hồi data chứa height và width")
    void testCheckLive_LiveUID() throws Exception {
        HttpRequestCall mockCall = Mockito.mock(HttpRequestCall.class, Mockito.RETURNS_SELF);
        when(sendRequest.get(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn("{\"data\":{\"height\":50,\"is_silhouette\":false,\"url\":\"https://fb.com/pic.jpg\",\"width\":50}}");

        BotStatus status = checkLiveUID.checkLive("100012345678");
        assertEquals(BotStatus.LIVE, status);
    }

    @Test
    @DisplayName("Trả về DEAD khi API phản hồi error từ Graph")
    void testCheckLive_DeadUID() throws Exception {
        HttpRequestCall mockCall = Mockito.mock(HttpRequestCall.class, Mockito.RETURNS_SELF);
        when(sendRequest.get(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn("{\"error\":{\"message\":\"Some of the aliases you requested do not exist\",\"type\":\"OAuthException\",\"code\":803}}");

        BotStatus status = checkLiveUID.checkLive("100099999999");
        assertEquals(BotStatus.DEAD, status);
    }

    @Test
    @DisplayName("Trả về ERROR khi có exception mạng hoặc phản hồi rỗng")
    void testCheckLive_NetworkException() throws Exception {
        HttpRequestCall mockCall = Mockito.mock(HttpRequestCall.class, Mockito.RETURNS_SELF);
        when(sendRequest.get(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new RuntimeException("Connection timed out"));

        BotStatus status = checkLiveUID.checkLive("100012345678");
        assertEquals(BotStatus.ERROR, status);
    }
}
