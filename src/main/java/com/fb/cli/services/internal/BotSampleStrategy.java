package com.fb.cli.services.internal;

import com.fb.cli.entities.Bot;

import java.util.List;

public interface BotSampleStrategy {

    /**
     * Thực hiện lấy danh sách Bot thỏa mãn điều kiện
     */
    List<Bot> sample();

    default List<Bot> sample(List<Bot> pool) {
        return sample();
    }
}
