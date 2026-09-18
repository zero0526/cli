package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryResult {

    private boolean success;
    private String storyId;
    private String postId;
    private String photoId;
    private List<String> photoIds;
    private String videoId;
    private List<String> videoIds;
    private String url;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;

    public static CreateStoryResult ok(String storyId, String postId, List<String> photoIds, List<String> videoIds, String url, String rawResponse) {
        String primaryPhotoId = (photoIds != null && !photoIds.isEmpty()) ? photoIds.get(0) : null;
        String primaryVideoId = (videoIds != null && !videoIds.isEmpty()) ? videoIds.get(0) : null;
        return CreateStoryResult.builder()
                .success(true)
                .storyId(storyId)
                .postId(postId)
                .photoId(primaryPhotoId)
                .photoIds(photoIds)
                .videoId(primaryVideoId)
                .videoIds(videoIds)
                .url(url)
                .rawResponse(rawResponse)
                .build();
    }

    public static CreateStoryResult ok(String storyId, String postId, String photoId, String url, String rawResponse) {
        List<String> pIds = (photoId != null && !photoId.isBlank()) ? List.of(photoId) : List.of();
        return ok(storyId, postId, pIds, List.of(), url, rawResponse);
    }

    public static CreateStoryResult fail(String errorCode, String errorMessage, String rawResponse) {
        return CreateStoryResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }
}
