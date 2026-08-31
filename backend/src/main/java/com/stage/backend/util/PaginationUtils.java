package com.stage.backend.util;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    /** Converts a 1-based API page query param to a 0-based Spring Data page index. */
    public static int toSpringPageIndex(int page) {
        return page - 1;
    }
}
