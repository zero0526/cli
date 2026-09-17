package com.fb.cli.services.external.interceptor;

import okhttp3.Request;

public interface RequestInterceptor {

    /**
     * Thứ tự ưu tiên thực thi của Interceptor (số nhỏ hơn chạy trước)
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Can thiệp và chỉnh sửa Request trước khi gửi đi
     */
    Request.Builder intercept(Request.Builder builder, RequestOptions options);
}
