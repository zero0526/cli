package com.fb.cli.services.external.proxy;

import java.util.List;

public interface ProxySampleStrategy<T> {
    List<T> sample(List<T> pool);

    default List<T> sample() {
        return sample(List.of());
    }
}
