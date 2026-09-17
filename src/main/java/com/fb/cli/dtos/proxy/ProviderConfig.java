package com.fb.cli.dtos.proxy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderConfig(
        @JsonProperty("code")
        @JsonAlias({"codes", "error_codes"})
        Map<String, Integer> code,

        @JsonProperty("endpoints")
        Map<String, EndpointConfig> endpoints
) {
    public Integer getCode(String label) {
        return code != null ? code.get(label) : null;
    }

    public boolean isCode(String label, Integer actualCode) {
        if (actualCode == null) return false;
        Integer expected = getCode(label);
        return actualCode.equals(expected);
    }
}
