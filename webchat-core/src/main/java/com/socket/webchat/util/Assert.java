package com.socket.webchat.util;

import java.util.function.Function;

/**
 * Assert异常扩展
 *
 * @date 2022/2/17
 */
public class Assert extends cn.hutool.core.lang.Assert {
    public static <X extends Throwable> void isTrue(boolean b, String m, Function<String, ? extends X> f) throws X {
        if (!b) {
            throw f.apply(m);
        }
    }

    public static <X extends Throwable> void isFalse(boolean b, String m, Function<String, ? extends X> f) throws X {
        if (b) {
            throw f.apply(m);
        }
    }

    public static <X extends Throwable> void isNull(Object object, String m, Function<String, ? extends X> f) throws X {
        if (object != null) {
            throw f.apply(m);
        }
    }

    public static <X extends Throwable> void notNull(Object object, String m, Function<String, ? extends X> f) throws X {
        if (object == null) {
            throw f.apply(m);
        }
    }
}
