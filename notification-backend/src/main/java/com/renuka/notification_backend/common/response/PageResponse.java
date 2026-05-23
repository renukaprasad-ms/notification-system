package com.renuka.notification_backend.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PageResponse(
            List<T> items,
            int page,
            int size,
            long totalItems,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}
