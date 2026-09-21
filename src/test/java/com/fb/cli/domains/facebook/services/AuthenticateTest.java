package com.fb.cli.domains.facebook.services;

import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthenticateTest {

    private SendRequest sendRequest;
    private Authenticate authenticate;

    @BeforeEach
    void setUp() {
        sendRequest = Mockito.mock(SendRequest.class);
        authenticate = new Authenticate(sendRequest);
    }

    @Test
    @DisplayName("extractTokenBM trích xuất thành công khi theo định dạng JS (dừng bởi '\",\"')")
    void testExtractTokenBM_JsFormat() {
        String html = "<html><script>var data = [\"EAAGNO4xyz1234567890abcdefghijklmn\",\"other_param\"];</script></html>";
        String token = authenticate.extractTokenBM(html);

        assertEquals("EAAGNO4xyz1234567890abcdefghijklmn", token);
    }

    @Test
    @DisplayName("extractTokenBM trích xuất thành công khi dùng Regex fallback (dấu nháy kép đơn thông thường)")
    void testExtractTokenBM_RegexFallback() {
        String html = "<script>{\"accessToken\":\"EAAGNO4testRegexToken1234567890abcdef\"}</script>";
        String token = authenticate.extractTokenBM(html);

        assertEquals("EAAGNO4testRegexToken1234567890abcdef", token);
    }

    @Test
    @DisplayName("extractTokenBM trả về null khi không có token EAAG")
    void testExtractTokenBM_NotFound() {
        String html = "<html><body>Please login to continue</body></html>";
        String token = authenticate.extractTokenBM(html);

        assertNull(token);
    }

    @Test
    @DisplayName("convertCookieToBMToken trả về null khi cookie rỗng")
    void testConvertCookieToBMToken_EmptyCookies() {
        assertNull(authenticate.convertCookieToBMToken(""));
        assertNull(authenticate.convertCookieToBMToken(null));
    }

    @Test
    @DisplayName("convertCookieToBMToken gửi request và lấy token thành công")
    void testConvertCookieToBMToken_Success() {
        HttpRequestCall mockCall = Mockito.mock(HttpRequestCall.class, Mockito.RETURNS_SELF);
        when(sendRequest.get(anyString())).thenReturn(mockCall);

        String sampleHtml = "window.__accessToken=\"EAAGNO4mockToken1234567890abcdef123456\";";
        when(mockCall.execute()).thenReturn(sampleHtml);

        String cookies = "c_user=1000123; xs=abc123xyz;";
        String token = authenticate.convertCookieToBMToken(cookies);

        assertNotNull(token);
        assertEquals("EAAGNO4mockToken1234567890abcdef123456", token);
    }
}
