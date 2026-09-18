package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.dtos.bot.BotDetermineCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.ReactionRequest;
import com.fb.cli.dtos.facebook.ReactionResult;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.FacebookReactionType;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FacebookWebReactionImplTest {

    private ActionConfigService mockActionConfigService;
    private ActionConfig sampleReactionConfig;

    @BeforeEach
    void setUp() {
        mockActionConfigService = Mockito.mock(ActionConfigService.class);

        sampleReactionConfig = ActionConfig.builder()
                .platform("FACEBOOK")
                .actionType("REACTION")
                .actionProvider("WEB_GRAPHQL")
                .endpointUrl("https://www.facebook.com/api/graphql/")
                .httpMethod("POST")
                .headers("{\"x-fb-friendly-name\":\"CometUFIFeedbackReactMutation\",\"x-fb-lsd\":\"{{lsdToken}}\"}")
                .formParams("{\"av\":\"{{userId}}\",\"doc_id\":\"8995964513767096\",\"variables\":\"{{PAYLOAD_VARIABLES}}\"}")
                .payloadTemplate("{\"input\":{\"feedback_id\":\"{{base64FeedbackId}}\",\"feedback_reaction_id\":\"{{reactionId}}\"}}")
                .extractorRules("{\"success_path\":\"data.feedback_react.feedback.id\",\"error_path\":\"errors[0].description\",\"error_code_path\":\"errors[0].code\"}")
                .build();

        when(mockActionConfigService.getActiveConfig("FACEBOOK", "REACTION", "WEB_GRAPHQL"))
                .thenReturn(sampleReactionConfig);
    }

    @Test
    @DisplayName("Thả reaction thành công khi Facebook GraphQL trả về data.feedback_react.feedback.id")
    void testSendReactionSuccess() {
        BotService mockBotService = Mockito.mock(BotService.class);
        FacebookWebContextService mockContextService = Mockito.mock(FacebookWebContextService.class);

        Bot bot = Bot.builder()
                .botId("10008888")
                .cookies("c_user=10008888; xs=sec88;")
                .userAgent("Mozilla/5.0")
                .build();
        when(mockBotService.getBot(any(BotSamplingConfig.class))).thenReturn(bot);

        FacebookWebContext context = FacebookWebContext.builder()
                .userId("10008888")
                .jazoest("2999")
                .lsdToken("LSD_123")
                .dtsgToken("DTSG_ABC")
                .build();
        when(mockContextService.resolveContext(eq(bot), any(), any())).thenReturn(context);

        String mockFbResponse = "{\"data\":{\"feedback_react\":{\"feedback\":{\"id\":\"feedback:123456_789\"}}}}";

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(mockFbResponse, okhttp3.MediaType.get("application/json")))
                        .build())
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        FacebookWebReactionImpl reactionAction = new FacebookWebReactionImpl(
                sendRequest, mockBotService, mockContextService, mockActionConfigService
        );

        ReactionRequest request = ReactionRequest.builder()
                .targetId("123456_789")
                .type(FacebookReactionType.LOVE)
                .botConfig(new BotDetermineCfg("facebook", "10008888"))
                .build();

        ReactionResult response = reactionAction.sendReaction(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("123456_789", response.getTargetId());
        assertEquals(FacebookReactionType.LOVE, response.getType());
    }

    @Test
    @DisplayName("Thả reaction thất bại khi Facebook GraphQL trả về lỗi (errors)")
    void testSendReactionGraphQLError() {
        BotService mockBotService = Mockito.mock(BotService.class);
        FacebookWebContextService mockContextService = Mockito.mock(FacebookWebContextService.class);

        Bot bot = Bot.builder()
                .botId("10008888")
                .cookies("c_user=10008888;")
                .build();
        when(mockBotService.getBot(any(BotSamplingConfig.class))).thenReturn(bot);

        FacebookWebContext context = FacebookWebContext.builder()
                .userId("10008888")
                .jazoest("2999")
                .lsdToken("LSD_123")
                .dtsgToken("DTSG_ABC")
                .build();
        when(mockContextService.resolveContext(eq(bot), any(), any())).thenReturn(context);

        String mockFbResponse = "{\"errors\":[{\"description\":\"Permission denied or rate limited\",\"code\":1357004}]}";

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(mockFbResponse, okhttp3.MediaType.get("application/json")))
                        .build())
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        FacebookWebReactionImpl reactionAction = new FacebookWebReactionImpl(
                sendRequest, mockBotService, mockContextService, mockActionConfigService
        );

        ReactionRequest request = ReactionRequest.builder()
                .targetId("123456_789")
                .type(FacebookReactionType.LIKE)
                .botConfig(new BotDetermineCfg("facebook", "10008888"))
                .build();

        ReactionResult response = reactionAction.sendReaction(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Permission denied or rate limited", response.getErrorMessage());
        assertEquals("1357004", response.getErrorCode());
    }

    @Test
    @DisplayName("Thả reaction trả về lỗi khi không tìm thấy bot")
    void testSendReactionNoBotFound() {
        BotService mockBotService = Mockito.mock(BotService.class);
        FacebookWebContextService mockContextService = Mockito.mock(FacebookWebContextService.class);
        when(mockBotService.getBot(any(BotSamplingConfig.class))).thenReturn(null);

        SendRequest sendRequest = new SendRequest(new OkHttpClient());

        FacebookWebReactionImpl reactionAction = new FacebookWebReactionImpl(
                sendRequest, mockBotService, mockContextService, mockActionConfigService
        );

        ReactionRequest request = ReactionRequest.builder()
                .targetId("123456_789")
                .type(FacebookReactionType.CARE)
                .botConfig(new BotDetermineCfg("facebook", "10008888"))
                .build();

        ReactionResult response = reactionAction.sendReaction(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("BOT_NOT_FOUND", response.getErrorCode());
    }
}
