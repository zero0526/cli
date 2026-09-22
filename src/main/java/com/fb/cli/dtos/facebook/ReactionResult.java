package com.fb.cli.dtos.facebook;

import com.fb.cli.enums.FacebookReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResult {

    private boolean success;
    private String targetId;
    private FacebookReactionType type;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static ReactionResult ok(String targetId, FacebookReactionType type, String rawResponse) {
        return ReactionResult.builder()
                .success(true)
                .targetId(targetId)
                .type(type)
                .rawResponse(rawResponse)
                .build();
    }

    public static ReactionResult fail(String targetId, FacebookReactionType type, String errorCode, String errorMessage, String rawResponse) {
        return ReactionResult.builder()
                .success(false)
                .targetId(targetId)
                .type(type)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}

