package com.fb.cli.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacebookReactionType {
    LIKE("1635855486666999"),
    LOVE("1678524932434102"),
    CARE("613557422527858"),
    HAHA("115940658764963"),
    WOW("478547315650144"),
    SAD("908563459236466"),
    ANGRY("444813342392137"),
    CANCEL("0");

    private final String id;

    public String getFbId() {
        return this.id;
    }

    public static FacebookReactionType fromNameOrId(String val) {
        if (val == null || val.isBlank()) {
            return LIKE;
        }
        for (FacebookReactionType type : values()) {
            if (type.name().equalsIgnoreCase(val) || type.id.equalsIgnoreCase(val)) {
                return type;
            }
        }
        return LIKE;
    }
}
