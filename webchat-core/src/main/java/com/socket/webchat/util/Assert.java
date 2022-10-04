package com.socket.webchat.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Assert异常扩展
 */
public class Assert {
    public static <X extends RuntimeException> void isTrue(boolean b, String m, Function<String, ? extends X> f) throws X {
        if (!b) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException> void isFalse(boolean b, String m, Function<String, ? extends X> f) throws X {
        if (b) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException> void isNull(Object obj, String m, Function<String, ? extends X> f) throws X {
        if (obj != null) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException> void notNull(Object obj, String m, Function<String, ? extends X> f) throws X {
        if (obj == null) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException> void isEmpty(CharSequence sequence, String m, Function<String, ? extends X> f) throws X {
        if (sequence != null && sequence.length() > 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException> void notEmpty(CharSequence sequence, String m, Function<String, ? extends X> f) throws X {
        if (sequence == null || sequence.length() == 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T> void isEmpty(T[] ts, String m, Function<String, ? extends X> f) throws X {
        if (ts != null && ts.length > 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T> void notEmpty(T[] ts, String m, Function<String, ? extends X> f) throws X {
        if (ts == null || ts.length == 0) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T> void equals(Object a, Object b, String m, Function<String, ? extends X> f) throws X {
        if (!Objects.equals(a, b)) {
            throw f.apply(m);
        }
    }

    public static <X extends RuntimeException, T> void notEquals(Object a, Object b, String m, Function<String, ? extends X> f) throws X {
        if (Objects.equals(a, b)) {
            throw f.apply(m);
        }
    }
}
