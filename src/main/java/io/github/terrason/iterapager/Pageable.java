package io.github.terrason.iterapager;

import javax.annotation.Nullable;

/**
 * 分页查询信息
 *
 * @author lipei
 */
public interface Pageable {
    /**
     * 偏移量
     */
    @Nullable
    Integer getOffset();

    default int offset() {
        Integer offset = getOffset();
        return offset == null ? 0 : offset;
    }

    @Nullable
    Integer getPage();

    default int page() {
        Integer page = getPage();
        return page == null ? 1 : page;
    }

    /**
     * 获取数据条数
     */
    int getLimit();

    /**
     * 当前是否开启分页.
     */
    boolean isPaged();

    /**
     * 未开启分页.
     */
    boolean isUnpaged();

    /**
     * 当前是第一页
     */
    boolean isFirstPage();

    /**
     * 切换到下一页
     */
    void next();

    /**
     * 切换到第一页
     */
    void first();

    /**
     * 当前页码已超出，无数据
     *
     * @param count 总数量
     * @return {@code true}-已超出，无数据
     */
    default boolean isExceed(int count) {
        Integer offset = getOffset();
        if (offset == null) {
            return count == 0;
        }
        return offset >= count;
    }
}
