package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVideoResult {

    private boolean success;
    private String videoId;
    private String uploadSessionId;
    private Long fileSize;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static UploadVideoResult ok(String videoId, String uploadSessionId, Long fileSize, String rawResponse) {
        return UploadVideoResult.builder()
                .success(true)
                .videoId(videoId)
                .uploadSessionId(uploadSessionId)
                .fileSize(fileSize)
                .rawResponse(rawResponse)
                .build();
    }

    public static UploadVideoResult fail(String errorCode, String errorMessage, String rawResponse) {
        return UploadVideoResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}
