package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.Bot;

public interface Cover {

    boolean setCover(String imgPath);

    default boolean setCover(String imgPath, Bot bot) {
        return setCover(imgPath, bot, null);
    }

    boolean setCover(String imgPath, Bot bot, ProxyInfo proxy);
}
