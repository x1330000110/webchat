package com.socket.secure.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Assert异常扩展
 */
public class Assert extends cn.hutool.core.lang.Assert {
    public static <X extends RuntimeException, M> void isTrue(boolean b, M m, Function<M, X> f) throws X {
        if (!b) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void isFalse(boolean b, M m, Function<M, X> f) throws X {
        if (b) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void isNull(Object obj, M m, Function<M, X> f) throws X {
        if (obj != null) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void notNull(Object obj, M m, Function<M, X> f) throws X {
        if (obj == null) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void isEmpty(CharSequence sequence, M m, Function<M, X> f) throws X {
        if (sequence != null && sequence.length() > 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void notEmpty(CharSequence sequence, M m, Function<M, X> f) throws X {
        if (sequence == null || sequence.length() == 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T, M> void isEmpty(T[] ts, M m, Function<M, X> f) throws X {
        if (ts != null && ts.length > 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T, M> void notEmpty(T[] ts, M m, Function<M, X> f) throws X {
        if (ts == null || ts.length == 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, M> void equals(Object a, Object b, M m, Function<M, X> f) throws X {
        if (!Objects.equals(a, b)) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T, M> void notEquals(Object a, Object b, M m, Function<M, X> f) throws X {
        if (Objects.equals(a, b)) {
            throw f.apply(m);
        }
    }
}
