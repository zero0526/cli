package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.dtos.bot.BotDetermineCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.ShareRequest;
import com.fb.cli.dtos.facebook.ShareResult;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.repositories.BotRepository;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Slf4j
class FacebookWebShareImplTest {
    @Autowired
    @Qualifier("facebookWebComment")
    private Comment facebookWebComment;

    @Autowired
    private BotService botService;

    @Autowired
    private BotRepository botRepository;

    @Autowired
    private FacebookWebContextService facebookWebContextService;

    @Autowired
    private ActionConfigService actionConfigService;

    @Autowired
    private SendRequest sendRequest;

    @Test
    @DisplayName("Share bài viết thành công khi Facebook GraphQL trả về story.id")
    void testSendShareSuccess() {

        FacebookWebShareImpl shareAction = new FacebookWebShareImpl(
                sendRequest, botService, facebookWebContextService, actionConfigService
        );

        ShareRequest request = ShareRequest.builder()
                .targetId("1426387813030419")
                .content("ngon luôn")
                .botConfig(new BotDetermineCfg("facebook", "61586296264033"))
                .build();

        ShareResult response = shareAction.sendShare(request);

        log.info("========== [KẾT QUẢ COMMENT THỰC TẾ VỚI BOT TỪ DB] ==========");
        log.info("Success      : {}", response.isSuccess());
        log.info("CommentId    : {}", response.getStoryId());
        log.info("ErrorCode    : {}", response.getErrorCode());
        log.info("ErrorMessage : {}", response.getErrorMessage());

        assertNotNull(response, "Kết quả trả về không được null");
    }
}
