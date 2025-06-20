package io.github.terrason.iterapager;

import javax.annotation.Nullable;

/**
 * PageProducer
 *
 * @author lipei
 */
public interface PageProducer<M> {
    /**
     * 生产下一页数据
     * @param pageable 分页参数
     * @param lastData 最后一次获取到的数据
     */
    Elem<M> next(Pageable pageable, @Nullable M lastData);

    interface Elem<M> {
        M getData();

        int getSize();

        default long total() {
            return 0;
        }
    }
}
