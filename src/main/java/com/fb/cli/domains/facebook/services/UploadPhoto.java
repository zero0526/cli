package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;

import java.io.File;

/**
 * Interface định nghĩa dịch vụ upload ảnh lên Facebook,
 * đảm bảo tính đa hình cho các provider khác nhau (Web Comet, App GraphQL, v.v.).
 */
public interface UploadPhoto {

    /**
     * Upload ảnh từ mảng byte
     */
    UploadImageResult uploadPhoto(Bot bot, byte[] imageBytes, String fileName, ProxyInfo proxy, RandomSamplingCfg proxySampler);

    /**
     * Upload ảnh từ File
     */
    UploadImageResult uploadPhoto(Bot bot, File file, ProxyInfo proxy, RandomSamplingCfg proxySampler);

    /**
     * Overload tiện ích: upload từ File với ProxyInfo
     */
    default UploadImageResult uploadPhoto(Bot bot, File file, ProxyInfo proxy) {
        return uploadPhoto(bot, file, proxy, null);
    }

    /**
     * Overload tiện ích: upload từ byte[] với ProxyInfo
     */
    default UploadImageResult uploadPhoto(Bot bot, byte[] imageBytes, String fileName, ProxyInfo proxy) {
        return uploadPhoto(bot, imageBytes, fileName, proxy, null);
    }

    /**
     * Overload tiện ích: upload từ File không cần proxy
     */
    default UploadImageResult uploadPhoto(Bot bot, File file) {
        return uploadPhoto(bot, file, null, null);
    }

    /**
     * Overload tiện ích: upload từ byte[] không cần proxy
     */
    default UploadImageResult uploadPhoto(Bot bot, byte[] imageBytes, String fileName) {
        return uploadPhoto(bot, imageBytes, fileName, null, null);
    }
}
