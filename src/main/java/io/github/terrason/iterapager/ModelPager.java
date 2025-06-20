package io.github.terrason.iterapager;

import com.google.common.collect.AbstractIterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * 分页处理工具
 *
 * @param <M> 分页查询结果类型
 */
public class ModelPager<M> implements Iterable<M> {

    protected final int batchSize;
    protected final PageProducer<M> producer;
    protected int timesLimit = 10000;

    /**
     * 分批次执行任务.
     *
     * @param batchSize 批处理大小
     * @param producer  数据源产生器. 一次只能产生batchSize对应的数据量，少了将停止循环，多了会产生bug。
     */
    @SuppressWarnings("unchecked")
    public ModelPager(int batchSize, PageProducer<? extends M> producer) {
        this.batchSize = batchSize;
        //noinspection rawtypes
        this.producer = (PageProducer<M>) producer;
    }


    /**
     * 分批次执行任务.
     *
     * @param pager    分页参数. 每次切换下一批次，修改该分页的页码
     * @param producer 数据源产生器. 一次只能产生{@link Pageable#getLimit() pager.limit}对应的数据量，少了将停止循环，多了会产生bug。
     * @param sizer    结果大小
     */
    public ModelPager(Pageable pager, UnaryOperator<M> producer, ToIntFunction<M> sizer) {
        this(pager.getLimit(), new PageableHoldProducer<>(pager, (oldData) -> {
            M modal = producer.apply(oldData);
            int size = sizer.applyAsInt(modal);
            return PageElements.of(modal, size);
        }));
    }

    /**
     * 设置最大循环次数，防止死循环。默认次数 10000.
     */
    public ModelPager<M> timesLimit(int limitTimes) {
        this.timesLimit = limitTimes;
        return this;
    }

    @Override
    public Iterator<M> iterator() {
        if (producer instanceof ModelPager.PageableHoldProducer) {
            //noinspection rawtypes
            Pageable pager = ((PageableHoldProducer<?>) producer).pager;
            return new PageIterator(pager);
        }
        return new PageIterator();
    }


    private class PageIterator extends AbstractIterator<M> {
        @Nullable
        protected M currentResult;
        private int times = 0;
        private long total = 0;
        private boolean exceed;
        private final Pageable pager;

        public PageIterator() {
            pager = PageQuery.of(1, batchSize);
        }

        public PageIterator(Pageable pager) {
            pager.first();
            this.pager = pager;
        }

        private void checkTimesLimit() {
            if (times > timesLimit) {
                throw new IllegalStateException("分页任务已处理 " + timesLimit + " 批次数据，超过最大循环次数！请分析可能存在的bug或调整最大循环次数。");
            }
        }

        @Override
        protected M computeNext() {
            if (currentResult != null) {//非第一页，切换页码
                pager.next();
            }
            if (exceed) {
                return endOfData();
            }
            checkTimesLimit();
            PageProducer.Elem<M> elements = producer.next(pager, currentResult);
            int size = elements.getSize();
            total += size;
            times += 1;

            if (size == 0) {
                return endOfData();
            }
            if (size > pager.getLimit()) {
                throw new UnsupportedOperationException(String.format("分页任务一次返回的数据量[%d]大于batchSize[%d]", size, pager.getLimit()));
            }
            if (size < pager.getLimit()) {
                exceed = true;
                return currentResult = elements.getData();
            }
            // size == pager.limit
            long totalLimit = elements.total();
            if (totalLimit > 0 && totalLimit <= total) {
                exceed = true;
            }
            return currentResult = elements.getData();
        }
    }

    @RequiredArgsConstructor
    private static class PageableHoldProducer<M> implements PageProducer<M> {
        @Getter
        private final Pageable pager;
        private final Function<M, PageProducer.Elem<M>> producer;

        @Override
        public PageProducer.Elem<M> next(Pageable pageable, @Nullable M lastData) {
            return producer.apply(lastData);
        }
    }
}
