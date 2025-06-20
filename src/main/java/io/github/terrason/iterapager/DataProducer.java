package io.github.terrason.iterapager;


import javax.annotation.Nullable;

@FunctionalInterface
public interface DataProducer<L, R> {
    R next(Pageable pagination, @Nullable L lastData);
}
