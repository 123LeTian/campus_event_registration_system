package com.example.campusevent.common.api;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, long page, long pageSize) {

    public long pages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }
}
