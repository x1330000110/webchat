package com.socket.secure.filter.validator.impl;

import cn.hutool.core.lang.Assert;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Repeated request validator based on {@link ConcurrentHashMap}</b> <br>
 * This validator contains data protection policies,
 * serialize the data to the file when the server is actively shut down, and restore when the server is restarted <br>
 * <b>Note:</b> When the server is passively shut down (kill task or unexpected downtime),
 * at least wait for the link expiration time before restarting,
 * otherwise you may be at risk of <b>replay attack</b>. <br>
 * <b>Note:</b> This implementation is based on JVM memory preservation,
 * and there may be risk of <b>{@link OutOfMemoryError}</b> when the instantaneous request is too large.
 *
 * @see SecureProperties#getLinkValidTime()
 * @see <a href="https://www.geeksforgeeks.org/replay-attack/">replay attack</a>
 */
public class MapRepeatValidator extends ConcurrentHashMap<String, Long> implements RepeatValidator, InitializingBean, DisposableBean {
    /**
     * Expired element cleanup threshold
     */
    private static final int QUERY_OVERFLOW = 100;
    /**
     * Effective time
     */
    private final transient SecureProperties properties;
    /**
     * Serialization cache file save path
     */
    private final transient File cacheFile;
    /**
     * Query count
     */
    private int queryCount;

    public MapRepeatValidator(File cacheFile, SecureProperties properties) {
        this.properties = properties;
        this.cacheFile = cacheFile;
        log.info("Map repeat validator is enable");
    }

    @Override
    public boolean isRepeated(long timestamp, String sign) {
        // Clean up expired elements
        if (++this.queryCount >= QUERY_OVERFLOW) {
            this.clearExpiredData();
            this.queryCount = 0;
        }
        // Save sign ID
        return putIfAbsent(sign, timestamp) != null;
    }

    /**
     * Clean up expired data
     */
    private void clearExpiredData() {
        int count = 0;
        long now = System.currentTimeMillis();
        for (Entry<String, Long> entry : super.entrySet()) {
            boolean exact = properties.isExactRequestTime();
            long time = entry.getValue();
            time = time * (exact ? 1 : 1000);
            if ((now - time) / 1000 > properties.getLinkValidTime()) {
                remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            log.info("Cleaner cleared {} pieces of expired requests", count);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (cacheFile.exists()) {
            // read serialized data
            log.info("Reading serialized file: {}", cacheFile.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                    super.putAll((MapRepeatValidator) ois.readObject());
                }
            }
            this.clearExpiredData();
            Assert.isTrue(cacheFile.delete(), () -> new BeanInitializationException("Serialized file is occupied"));
        }
    }

    @Override
    public void destroy() throws Exception {
        this.clearExpiredData();
        if (!isEmpty()) {
            // serialized save
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(this);
                }
            }
        }
    }
}
