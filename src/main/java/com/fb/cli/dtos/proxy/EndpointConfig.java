package com.fb.cli.dtos.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EndpointConfig(
        @JsonProperty("endpoint_name")
        String endpointName,

        @JsonProperty("method")
        String method,

        @JsonProperty("path")
        String path,

        @JsonProperty("timeout_ms")
        Long timeoutMs,

        @JsonProperty("retry")
        RetryConfig retry,

        @JsonProperty("headers")
        Map<String, String> headers,

        @JsonProperty("query_params")
        Map<String, String> queryParams,

        @JsonProperty("body_template")
        String bodyTemplate,

        @JsonProperty("response_mapping")
        ResponseMappingConfig responseMapping
) {
}
