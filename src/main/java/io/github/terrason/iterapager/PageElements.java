package io.github.terrason.iterapager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * PageElements
 *
 * @author lipei
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageElements {
    public static <T> PageProducer.Elem<T> of(T data, int size){
        return new DefaultElement<>(data, size);
    }
    public static <T> PageProducer.Elem<T> of(T data, int size, long total){
        return new FullElement<>(data, size, total);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class DefaultElement<T> implements PageProducer.Elem<T> {
        private final T data;
        private final int size;
    }


    public static class FullElement<T> extends DefaultElement<T> {
        private final long total;

        private FullElement(T data, int size, long total) {
            super(data, size);
            this.total = total;
        }

        @Override
        public long total() {
            return total;
        }
    }
}
