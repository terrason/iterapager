package io.github.terrason.iterapager;

import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class DelegatedPageProducer<M, D> implements PageProducer<M> {
    private final DataProducer<M, D> producer;
    private final Function<D, M> elementMapper;
    private final ToIntFunction<D> sizeMapper;
    @Nullable
    private final ToLongFunction<D> totalMapper;

    public DelegatedPageProducer(DataProducer<M, D> producer, Function<D, M> elementMapper, ToIntFunction<D> sizeMapper, @Nullable ToLongFunction<D> totalMapper) {
        this.producer = producer;
        this.elementMapper = elementMapper;
        this.sizeMapper = sizeMapper;
        this.totalMapper = totalMapper;
    }

    static <E> DelegatedPageProducer<Collection<E>, Collection<E>> ofCollection(DataProducer<E, ? extends Collection<E>> producer) {
        DataProducer<Collection<E>, Collection<E>> dp = (pageable, oldCollection) -> {
            E lastElement = oldCollection == null || oldCollection.isEmpty() ? null : Iterables.getLast(oldCollection);
            return producer.next(pageable, lastElement);
        };
        return new DelegatedPageProducer<>(dp, a -> a, Collection::size, null);
    }

    static <M> DelegatedPageProducer<M, M> ofModal(DataProducer<M, M> producer, ToIntFunction<M> sizeMapper) {
        return new DelegatedPageProducer<>(producer, a -> a, sizeMapper, null);
    }

    @Override
    public Elem<M> next(Pageable pageable, @Nullable M oldData) {
        D delegation = producer.next(pageable, oldData);
        long totalLimit = totalMapper == null ? 0 : totalMapper.applyAsLong(delegation);
        return PageElements.of(
                elementMapper.apply(delegation),
                sizeMapper.applyAsInt(delegation),
                totalLimit);
    }
}