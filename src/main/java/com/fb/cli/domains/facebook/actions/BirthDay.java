package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.Bot;

public interface BirthDay {

    boolean updateBirthDay();

    default boolean updateBirthDay(int day, int month, int year) {
        return updateBirthDay(day, month, year, null, null);
    }

    default boolean updateBirthDay(int day, int month, int year, Bot bot) {
        return updateBirthDay(day, month, year, bot, null);
    }

    boolean updateBirthDay(int day, int month, int year, Bot bot, ProxyInfo proxy);
}
