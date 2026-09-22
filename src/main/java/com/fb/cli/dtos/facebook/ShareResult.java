package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResult {

    private boolean success;
    private String targetId;
    private String storyId;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static ShareResult ok(String targetId, String storyId, String rawResponse) {
        return ShareResult.builder()
                .success(true)
                .targetId(targetId)
                .storyId(storyId)
                .rawResponse(rawResponse)
                .build();
    }

    public static ShareResult fail(String targetId, String errorCode, String errorMessage, String rawResponse) {
        return ShareResult.builder()
                .success(false)
                .targetId(targetId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}
