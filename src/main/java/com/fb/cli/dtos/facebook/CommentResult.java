package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResult {

    private boolean success;
    private String targetId;
    private String commentId;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static CommentResult ok(String targetId, String commentId, String rawResponse) {
        return CommentResult.builder()
                .success(true)
                .targetId(targetId)
                .commentId(commentId)
                .rawResponse(rawResponse)
                .build();
    }

    public static CommentResult fail(String targetId, String errorCode, String errorMessage, String rawResponse) {
        return CommentResult.builder()
                .success(false)
                .targetId(targetId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}

