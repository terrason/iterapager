package com.capsulode.util.iterapager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * 分页处理工具
 *
 * @param <T> 列表元素类型
 */
public class IteraPager<T> extends ModelPager<Collection<T>> {
    private IteraPager(int batchSize, PageProducer<? extends Collection<T>> producer) {
        super(batchSize, producer);
    }

    /**
     * 分批次执行任务.
     *
     * @param batchSize 批处理大小
     * @param producer  数据源产生器. 一次只能产生batchSize对应的数据量，少了将停止循环，多了会产生bug。
     */
    public IteraPager(int batchSize, Function<Pageable, ? extends Collection<T>> producer) {
        super(batchSize, DelegatedPageProducer.ofCollection((p, ignore) -> producer.apply(p)));
    }

    /**
     * 按顺序查询的分批数据
     *
     * @param <E>       数据类型
     * @param batchSize 批处理大小
     * @param producer  数据源产生器. 一次只能产生batchSize对应的数据量，少了将停止循环，多了会产生bug。注意这个数据产生器接收一个额外参数last，表示上一批数据的最后一个元素，第一次生产数据时，这个参数为{@code null}。
     *                 这个参数可以用来实现有序查询。
     *                 例如：如果数据是有序的，可以用last参数来获取上一批数据的最后一个元素，使用这个元素的ID和ID排序，查询下一批数据将极大提高查询性能。
     * @return 按顺序查询的分批数据
     */
    public static <E> IteraPager<E> ofOrdered(int batchSize, DataProducer<E, ? extends Collection<E>> producer) {
        return new IteraPager<>(batchSize, DelegatedPageProducer.ofCollection(producer));
    }

    public static <D, T> IteraPager<T> of(int batchSize,
            Function<Pageable, D> producer,
            Function<D, Collection<T>> elementsMapper,
            ToIntFunction<D> sizeMapper,
            @Nullable ToLongFunction<D> totalLimitMapper) {
        DataProducer<Collection<T>, D> delegatedProducer = (pageable, lastElements) -> producer.apply(pageable);
        return new IteraPager<>(batchSize, new DelegatedPageProducer<>(
                delegatedProducer,
                elementsMapper,
                sizeMapper,
                totalLimitMapper));
    }

    public static <D, T> IteraPager<T> ofOrdered(int batchSize,
            DataProducer<T, D> producer,
            Function<D, Collection<T>> elementsMapper,
            ToIntFunction<D> sizeMapper,
            @Nullable ToLongFunction<D> totalLimitMapper) {
        DataProducer<Collection<T>, D> delegatedProducer = (pageable, lastElements) -> {
            T lastElement = lastElements == null || lastElements.isEmpty() ? null : Iterables.getLast(lastElements);
            return producer.next(pageable, lastElement);
        };
        return new IteraPager<>(batchSize, new DelegatedPageProducer<>(
                delegatedProducer,
                elementsMapper,
                sizeMapper,
                totalLimitMapper));
    }

    /**
     * 分批次执行任务.
     *
     * @param pager    分页参数. 每次切换下一批次，修改该分页的页码
     * @param producer 数据源产生器. 一次只能产生{@link Pageable#getLimit()
     *                 pager.limit}对应的数据量，少了将停止循环，多了会产生bug。
     */
    public IteraPager(Pageable pager, Supplier<Collection<? extends T>> producer) {
        super(pager, (oldElements) -> Collections.unmodifiableCollection(producer.get()), Collection::size);
    }

    /**
     * 设置最大循环次数，防止死循环。默认次数 100000.
     */
    @Override
    public IteraPager<T> timesLimit(int limitTimes) {
        this.timesLimit = limitTimes;
        return this;
    }

    public Iterator<T> concat() {
        return Iterators.concat(
                Iterators.transform(
                        iterator(),
                        Collection::iterator));
    }

    public <D> Iterator<D> concat(Function<? super T, ? extends D> mapper) {
        Iterator<T> fromIter = concat();
        return Iterators.transform(fromIter, t -> mapper.apply(t));
    }

    public Iterable<T> flat() {
        return this::concat;
    }

    public <D> Iterable<D> flat(Function<T, D> mapper) {
        return Iterables.transform(flat(), t -> mapper.apply(t));
    }
}
