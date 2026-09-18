package com.fb.cli.dtos.facebook;

import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryRequest {

    public enum MediaType {
        PHOTO, VIDEO
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaItem {
        private MediaType type;
        /**
         * ID nếu media đã được upload trước (photoId hoặc videoId)
         */
        private String id;
        /**
         * Dữ liệu nhị phân của media
         */
        private byte[] bytes;
        /**
         * File media trên máy
         */
        private File file;
        /**
         * Đường dẫn file media
         */
        private String filePath;
        /**
         * Tên file kèm đuôi mở rộng
         */
        private String fileName;

        public static MediaItem photo(String photoId) {
            return MediaItem.builder().type(MediaType.PHOTO).id(photoId).build();
        }

        public static MediaItem photo(byte[] bytes, String fileName) {
            return MediaItem.builder().type(MediaType.PHOTO).bytes(bytes).fileName(fileName).build();
        }

        public static MediaItem photo(File file) {
            return MediaItem.builder().type(MediaType.PHOTO).file(file).build();
        }

        public static MediaItem photo(String filePath, String fileName) {
            return MediaItem.builder().type(MediaType.PHOTO).filePath(filePath).fileName(fileName).build();
        }

        public static MediaItem video(String videoId) {
            return MediaItem.builder().type(MediaType.VIDEO).id(videoId).build();
        }

        public static MediaItem video(byte[] bytes, String fileName) {
            return MediaItem.builder().type(MediaType.VIDEO).bytes(bytes).fileName(fileName).build();
        }

        public static MediaItem video(File file) {
            return MediaItem.builder().type(MediaType.VIDEO).file(file).build();
        }

        public static MediaItem video(String filePath, String fileName) {
            return MediaItem.builder().type(MediaType.VIDEO).filePath(filePath).fileName(fileName).build();
        }
    }

    /**
     * Nội dung caption của bài viết
     */
    private String content;

    /**
     * Danh sách media hỗn hợp tuỳ ý (giữ nguyên thứ tự ảnh / video khi render)
     */
    private java.util.List<MediaItem> mediaItems;

    // ===== Tiện ích cho Photo (Đơn hoặc Đa ảnh) =====
    /**
     * Photo ID nếu ảnh đã được upload trước đó
     */
    private String photoId;

    /**
     * Danh sách photo ID đã upload trước đó
     */
    private java.util.List<String> photoIds;

    /**
     * Dữ liệu ảnh dạng byte array (tự động upload nếu chưa có photoId)
     */
    private byte[] imageBytes;

    /**
     * Danh sách dữ liệu ảnh dạng byte array
     */
    private java.util.List<byte[]> imageBytesList;

    /**
     * File ảnh trên máy (tự động đọc và upload nếu chưa có photoId)
     */
    private File imageFile;

    /**
     * Danh sách file ảnh trên máy
     */
    private java.util.List<File> imageFiles;

    /**
     * Đường dẫn file ảnh
     */
    private String imagePath;

    /**
     * Danh sách đường dẫn file ảnh
     */
    private java.util.List<String> imagePaths;

    /**
     * Tên file ảnh kèm phần mở rộng (ví dụ: photo.png)
     */
    @Builder.Default
    private String imageName = "image.png";

    // ===== Tiện ích cho Video (Đơn hoặc Đa video) =====
    /**
     * Video ID nếu video đã được upload trước đó
     */
    private String videoId;

    /**
     * Danh sách video ID đã upload trước đó
     */
    private java.util.List<String> videoIds;

    /**
     * Dữ liệu video dạng byte array (tự động upload nếu chưa có videoId)
     */
    private byte[] videoBytes;

    /**
     * Danh sách dữ liệu video dạng byte array
     */
    private java.util.List<byte[]> videoBytesList;

    /**
     * File video trên máy (tự động upload nếu chưa có videoId)
     */
    private File videoFile;

    /**
     * Danh sách file video trên máy
     */
    private java.util.List<File> videoFiles;

    /**
     * Đường dẫn file video
     */
    private String videoPath;

    /**
     * Danh sách đường dẫn file video
     */
    private java.util.List<String> videoPaths;

    /**
     * Tên file video kèm phần mở rộng (ví dụ: video.mp4)
     */
    @Builder.Default
    private String videoName = "video.mp4";

    /**
     * Quyền riêng tư: EVERYONE, FRIENDS, SELF (mặc định FRIENDS)
     */
    @Builder.Default
    private String privacy = "FRIENDS";

    /**
     * Bot cụ thể thực hiện
     */
    private Bot bot;

    /**
     * Cấu hình bốc Bot
     */
    private BotSamplingConfig botConfig;

    /**
     * Proxy cụ thể
     */
    private ProxyInfo proxy;

    /**
     * Cấu hình bốc Proxy
     */
    private RandomSamplingCfg proxySampler;
}
