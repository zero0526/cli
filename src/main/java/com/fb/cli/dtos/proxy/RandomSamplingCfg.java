package com.fb.cli.dtos.proxy;

import com.fb.cli.enums.RotationType;

public record RandomSamplingCfg(
        int sampleSize,
        long seed,
        RotationType proxyType
) {
}
