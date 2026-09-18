package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.dtos.facebook.CommentRequest;
import com.fb.cli.dtos.facebook.CommentResult;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import com.fb.cli.services.internal.BotService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class FacebookWebCommentImplTest {

    @Autowired
    @Qualifier("facebookWebComment")
    private Comment facebookWebComment;

    @Autowired
    private BotService botService;

    @Autowired
    private BotRepository botRepository;

    @Test
    @DisplayName("Lấy bot trực tiếp từ DB với platform FACEBOOK và thực hiện comment thật")
    void testRealCommentWithBotFromDb() {
        // 1. Kéo bot trực tiếp từ Database với platform FACEBOOK
        List<Bot> facebookBots = botRepository.findByPlatformAndStatus(
                "FACEBOOK",
                BotStatus.ACTIVE,
                PageRequest.of(0, 5)
        ).getContent();

        log.info("========== [TEST THỰC TẾ: LẤY BOT TRỰC TIẾP TỪ DB] ==========");
        log.info("Số lượng bot FACEBOOK ACTIVE tìm thấy trong DB: {}", facebookBots.size());
        assertFalse(facebookBots.isEmpty(), "Không tìm thấy Bot platform FACEBOOK trong database");

        Bot bot = facebookBots.get(0);
        log.info("Sử dụng Bot từ DB: botId={}, platform={}, status={}", bot.getBotId(), bot.getPlatform(), bot.getStatus());
        assertNotNull(bot.getCookies(), "Bot cần có cookies để tương tác web");

        // 2. Target post & nội dung comment
        String targetPostId = "1431152365591049";
        String commentContent = "Buồn quá";

        CommentRequest request = CommentRequest.builder()
                .targetId(targetPostId)
                .content(commentContent)
                .bot(bot)
                .build();

        // 3. Thực thi comment
        CommentResult result = facebookWebComment.sendComment(request);

        log.info("========== [KẾT QUẢ COMMENT THỰC TẾ VỚI BOT TỪ DB] ==========");
        log.info("Success      : {}", result.isSuccess());
        log.info("CommentId    : {}", result.getCommentId());
        log.info("ErrorCode    : {}", result.getErrorCode());
        log.info("ErrorMessage : {}", result.getErrorMessage());

        assertNotNull(result, "Kết quả trả về không được null");
    }

    @Test
    @DisplayName("Gửi comment thật tự động kéo bot platform FACEBOOK qua BotRandomCfg")
    void testRealCommentWithBotRandomCfg() {
        String targetPostId = "pfbid0XVjN5ehH6YHFF6CdZtupgrFgzQbFDP6eMpBu66dU3MqZQ7FYDuE1m2eiou5rmtCvl";
        String commentContent = "Test comment qua BotRandomCfg('FACEBOOK') lúc " + LocalDateTime.now();

        CommentRequest request = CommentRequest.builder()
                .targetId(targetPostId)
                .content(commentContent)
                .botConfig(new BotRandomCfg("FACEBOOK"))
                .build();

        CommentResult result = facebookWebComment.sendComment(request);

        log.info("========== [KẾT QUẢ COMMENT QUA BOT_RANDOM_CFG] ==========");
        log.info("Success      : {}", result.isSuccess());
        log.info("CommentId    : {}", result.getCommentId());
        log.info("ErrorCode    : {}", result.getErrorCode());
        log.info("ErrorMessage : {}", result.getErrorMessage());

        assertNotNull(result, "Kết quả trả về không được null");
    }
}
