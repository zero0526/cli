package com.fb.cli.dtos.proxy;

public record ProxyInfo(
        String host,
        Integer port,
        String username,
        String password,
        String protocol,
        Long expireAt
) {
}
