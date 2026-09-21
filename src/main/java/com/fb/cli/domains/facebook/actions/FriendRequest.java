package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.FBFriendReqStatus;

public interface FriendRequest {

    FBFriendReqStatus sendFriendReq(String targetFriendTarget);

    default FBFriendReqStatus sendFriendReq(String targetFriendTarget, Bot bot) {
        return sendFriendReq(targetFriendTarget, bot, null);
    }

    FBFriendReqStatus sendFriendReq(String targetFriendTarget, Bot bot, ProxyInfo proxy);
}
