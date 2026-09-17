package com.fb.cli.dtos.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseMappingConfig(
        @JsonProperty("root_path")
        String rootPath,

        @JsonProperty("host")
        String host,

        @JsonProperty("port")
        String port,

        @JsonProperty("username")
        String username,

        @JsonProperty("password")
        String password,

        @JsonProperty("protocol")
        String protocol,

        @JsonProperty("default_protocol")
        String defaultProtocol,

        @JsonProperty("expire_at")
        String expireAt,

        @JsonProperty("raw_proxy")
        String rawProxy,

        @JsonProperty("raw_proxy_format")
        String rawProxyFormat
) {
}
