package com.fb.cli.dtos.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record RetryConfig(
        @JsonProperty("max_attempts")
        Integer maxAttempts,

        @JsonProperty("backoff_ms")
        Long backoffMs,

        @JsonProperty("multiplier")
        Double multiplier
) {
    public RetryConfig {
        if (maxAttempts == null || maxAttempts <= 0) {
            maxAttempts = 1;
        }
        if (backoffMs == null || backoffMs < 0) {
            backoffMs = 1000L;
        }
        if (multiplier == null || multiplier < 1.0) {
            multiplier = 1.0;
        }
    }
}
