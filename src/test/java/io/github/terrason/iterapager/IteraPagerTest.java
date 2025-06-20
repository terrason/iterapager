package io.github.terrason.iterapager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class IteraPagerTest {

    private List<Integer> data;

    private Function<Pageable, List<Integer>> createProducer(int batchSize, @Nullable IntConsumer onNElementsProduce) {
        return pageable -> {
            int start = (pageable.page() - 1) * batchSize;
            if (start >= data.size())
                return Collections.emptyList();
            int end = Math.min(start + batchSize, data.size());
            if (onNElementsProduce != null) {
                onNElementsProduce.accept(end - start);
            }

            return data.subList(start, end);
        };
    }

    private DataProducer<Integer, List<Integer>> createOrderedProducer(int batchSize) {
        return (pageable, last) -> {
            int lastIndex = last == null ? -1 : data.indexOf(last);
            int start = lastIndex + 1;
            if (start >= data.size())
                return Collections.emptyList();
            int end = Math.min(start + batchSize, data.size());
            return data.subList(start, end);
        };
    }

    @BeforeEach
    void setUp() {
        data = IntStream.rangeClosed(1, 100)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * 测试 IteraPager 构造函数和 concat() 方法。
     * <pre>
     * 验证目标：
     * 1. 确保 IteraPager 实例能够按顺序正确检索所有元素。
     * 2. 确保生产者被正确调用，并验证生产的元素数量是否与得到的数据大小一致。
     * </pre>
     */
    @Test
    void testConstructorAndConcat() {
        int batchSize = 10;
        AtomicInteger callCount = new AtomicInteger(0);
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, callCount::addAndGet);
        IteraPager<Integer> pager = new IteraPager<>(batchSize, producer);

        List<Integer> result = new ArrayList<>();
        pager.concat().forEachRemaining(result::add);

        assertEquals(data, result);
        assertEquals(data.size(), callCount.get());
    }

    /**
     * 测试 ofOrdered() 方法。
     * <p>
     * 验证目标：
     * 1. 确保按顺序分页能够正确检索所有元素。
     */
    @Test
    void testOfOrdered() {
        int batchSize = 4;
        DataProducer<Integer, List<Integer>> orderedProducer = createOrderedProducer(batchSize);
        IteraPager<Integer> pager = IteraPager.ofOrdered(batchSize, orderedProducer);

        List<Integer> result = new ArrayList<>();
        pager.concat().forEachRemaining(result::add);

        assertEquals(data, result);
    }

    /**
     * 测试 of() 方法。
     * <p>
     * 验证目标：
     * 1. 确保通过元素映射器和大小映射器能够正确检索所有元素。
     */
    @Test
    void testOfWithMappers() {
        int batchSize = 3;
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, null);
        IteraPager<Integer> pager = IteraPager.of(
                batchSize,
                pageable -> {
                    List<Integer> elements = producer.apply(pageable);
                    return new PageResult(elements, elements.size());
                },
                PageResult::getElements,
                PageResult::getSize,
                null);

        List<Integer> result = new ArrayList<>();
        pager.concat().forEachRemaining(result::add);

        assertEquals(data, result);
    }

    /**
     * 测试 concat(Function) 方法。
     * <p>
     * 验证目标：
     * 1. 确保在合并批次时正确应用映射器。
     */
    @Test
    void testConcatWithMapper() {
        int batchSize = 3;
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, null);
        IteraPager<Integer> pager = new IteraPager<>(batchSize, producer);

        List<String> mapped = new ArrayList<>();
        pager.concat(Object::toString).forEachRemaining(mapped::add);

        List<String> expected = data.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        assertEquals(expected, mapped);
    }

    /**
     * 测试 flat() 方法。
     * <p>
     * 验证目标：
     * 1. 确保返回的可迭代对象能够正确遍历所有元素。
     */
    @Test
    void testFlat() {
        int batchSize = 4;
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, null);
        IteraPager<Integer> pager = new IteraPager<>(batchSize, producer);

        List<Integer> result = new ArrayList<>();
        for (Integer i : pager.flat()) {
            result.add(i);
        }
        assertEquals(data, result);
    }

    /**
     * 测试 flat(Function) 方法。
     * <p>
     * 验证目标：
     * 1. 确保在遍历所有元素时正确应用映射器。
     */
    @Test
    void testFlatWithMapper() {
        int batchSize = 2;
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, null);
        IteraPager<Integer> pager = new IteraPager<>(batchSize, producer);

        List<String> result = new ArrayList<>();
        for (String s : pager.flat(Object::toString)) {
            result.add(s);
        }
        List<String> expected = data.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        assertEquals(expected, result);
    }

    /**
     * 测试 timesLimit() 方法。
     * <p>
     * 验证目标：
     * 1. 确保当生产批次超出最大循环次数限制时能抛出异常。
     */
    @Test
    void testTimesLimit() {
        int batchSize = 2;
        int limitTimes = 50;
        Function<Pageable, List<Integer>> producer = createProducer(batchSize, null);
        IteraPager<Integer> pager = new IteraPager<>(batchSize, producer);
        IteraPager<Integer> returned = pager.timesLimit(limitTimes);
        assertSame(pager, returned);
        assertEquals(limitTimes, pager.timesLimit);

        List<Integer> result = new ArrayList<>();
        for (Integer i : pager.flat()) {
            result.add(i);
        }
        assertEquals(data, result);

        limitTimes = limitTimes - 1;
        pager.timesLimit(limitTimes);
        assertThrowsExactly(IllegalStateException.class, () -> {
            for (Integer i : pager.flat()) {
                result.add(i);
            }
        });
    }

    static class PageResult {
        private final Collection<Integer> elements;
        private final int size;

        PageResult(Collection<Integer> elements, int size) {
            this.elements = elements;
            this.size = size;
        }

        public Collection<Integer> getElements() {
            return elements;
        }

        public int getSize() {
            return size;
        }
    }
}