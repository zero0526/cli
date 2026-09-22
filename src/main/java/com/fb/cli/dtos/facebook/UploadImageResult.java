package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResult {

    private boolean success;
    private String photoId;
    private String imageSrc;
    private Integer width;
    private Integer height;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static UploadImageResult ok(String photoId, String imageSrc, Integer width, Integer height, String rawResponse) {
        return UploadImageResult.builder()
                .success(true)
                .photoId(photoId)
                .imageSrc(imageSrc)
                .width(width)
                .height(height)
                .rawResponse(rawResponse)
                .build();
    }

    public static UploadImageResult fail(String errorCode, String errorMessage, String rawResponse) {
        return UploadImageResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}
