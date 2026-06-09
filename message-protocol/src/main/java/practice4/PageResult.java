package practice4;

import java.util.List;

public class PageResult<T> {
    private final List<T> items;
    private final long totalCount;
    private final int page;
    private final int pageSize;

    public PageResult(List<T> items, long totalCount, int page, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public long getTotalCount() { return totalCount; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    public int getTotalPages() { return pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize); }

    public boolean hasNextPage() { return page < getTotalPages() - 1; }
    public boolean isEmpty() { return items.isEmpty(); }
}