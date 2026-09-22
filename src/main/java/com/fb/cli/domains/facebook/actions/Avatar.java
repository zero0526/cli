package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.Bot;

public interface Avatar {

    boolean setAvatar(String imgPath);

    default boolean setAvatar(String imgPath, Bot bot) {
        return setAvatar(imgPath, bot, null);
    }

    boolean setAvatar(String imgPath, Bot bot, ProxyInfo proxy);
}
