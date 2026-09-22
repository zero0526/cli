package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.Bot;

public interface Username {

    boolean changeName(String firstName, String middleName, String lastName);

    default boolean changeName(String firstName, String middleName, String lastName, Bot bot) {
        return changeName(firstName, middleName, lastName, bot, null);
    }

    boolean changeName(String firstName, String middleName, String lastName, Bot bot, ProxyInfo proxy);
}
