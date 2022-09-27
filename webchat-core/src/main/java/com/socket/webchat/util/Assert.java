package com.socket.webchat.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Assert异常扩展
 */
public class Assert extends cn.hutool.core.lang.Assert {
    public static <X extends Throwable> void isTrue(boolean b, String m, Function<String, ? extends X> f) throws X {
        if (!b) {
            throw f.apply(m);
        }
    }

    public static <X extends Throwable> void isFalse(boolean b, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(!b, m, f);
    }

    public static <X extends Throwable> void isNull(Object object, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(object == null, m, f);
    }

    public static <X extends Throwable> void notNull(Object object, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(object != null, m, f);
    }

    public static <X extends Throwable> void isEmpty(CharSequence sequence, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(sequence == null || sequence.length() == 0, m, f);
    }

    public static <X extends Throwable> void notEmpty(CharSequence sequence, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(sequence != null && sequence.length() > 0, m, f);
    }

    public static <X extends Throwable, T> void isEmpty(T[] ts, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(ts == null || ts.length == 0, m, f);
    }

    public static <X extends Throwable, T> void notEmpty(T[] ts, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(ts != null && ts.length > 0, m, f);
    }

    public static <X extends Throwable, T> void equals(Object a, Object b, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(Objects.equals(a, b), m, f);
    }

    public static <X extends Throwable, T> void notEquals(Object a, Object b, String m, Function<String, ? extends X> f) throws X {
        Assert.isTrue(!Objects.equals(a, b), m, f);
    }
}
