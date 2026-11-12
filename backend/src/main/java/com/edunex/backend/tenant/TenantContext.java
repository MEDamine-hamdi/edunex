package com.edunex.backend.tenant;

import java.util.function.Supplier;

/** The university of the current request. Empty = no restriction (platform administrator, login, background jobs). */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static long currentOrZero() {
        Long v = CURRENT.get();
        return v == null ? 0L : v;
    }

    public static void set(Long id) {
        if (id == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(id);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T call(Long id, Supplier<T> action) {
        Long previous = CURRENT.get();
        set(id);
        try {
            return action.get();
        } finally {
            set(previous);
        }
    }

    public static void run(Long id, Runnable action) {
        call(id, () -> {
            action.run();
            return null;
        });
    }
}