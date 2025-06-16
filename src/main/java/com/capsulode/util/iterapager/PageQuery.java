package com.capsulode.util.iterapager;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

/**
 * 分页查询条件
 *
 * @author lipei
 */
@Getter
@Setter
public class PageQuery implements Pageable {
    static final int DEFAULT_LIMIT = 20;

    @Nullable
    protected Integer page;

    protected int limit = DEFAULT_LIMIT;

    public int requirePage() {
        if (page == null) {
            throw new IllegalStateException("需要分页");
        }
        return page;
    }

    @Override
    public String toString() {
        return String.format("第%d页（每页%d条）", page, limit);
    }

    @Nullable
    public Integer getOffset() {
        if (isUnpaged()) {
            return null;
        }
        //noinspection ConstantConditions
        int p = Math.max(this.page, 1);
        int l = Math.max(this.limit, 1);
        return (p - 1) * l;
    }


    /**
     * 当前是否开启分页.
     */
    public boolean isPaged() {
        return page != null;
    }

    /**
     * 未开启分页.
     */
    public boolean isUnpaged() {
        return page == null;
    }

    @Override
    public boolean isFirstPage() {
        return isUnpaged() || Integer.valueOf(1).equals(getPage());
    }

    /**
     * 返回下一页对象.
     */
    @Override
    public void next() {
        if (isUnpaged()) {
            throw new NoSuchElementException();
        }
        assert page != null;
        setPage(page + 1);
    }

    @Override
    public void first() {
        if (isPaged()) {
            page = 1;
        }
    }

    public static PageQuery of(int page, int limit) {
        PageQuery query = new PageQuery();
        query.setPage(page);
        query.setLimit(limit);
        return query;
    }

    public static final PageQuery ALL = new QueryAll();

    private static class QueryAll extends PageQuery{

        @Override
        public void setPage(@Nullable Integer page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLimit(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void next() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void first() {
            throw new UnsupportedOperationException();
        }
    }

    public static PageQuery top(int limit) {
        return new AlwaysTop(limit);
    }

    private static class AlwaysTop extends PageQuery {
        AlwaysTop(int limit) {
            page = 1;
            this.limit = limit;
        }

        @Override
        public void setPage(@Nullable Integer page) {
            //page always be 1, ignoring...
        }

        @Override
        public void next() {
            //page always be 1, ignoring...
        }

        @Override
        public void first() {
            //page always be 1, ignoring...
        }
    }
}
