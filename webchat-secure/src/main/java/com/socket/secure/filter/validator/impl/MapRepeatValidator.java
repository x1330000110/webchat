package com.socket.secure.filter.validator.impl;

import cn.hutool.core.util.HexUtil;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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
public class MapRepeatValidator implements RepeatValidator, InitializingBean {
    /**
     * Expired element cleanup threshold
     */
    private static final int QUERY_OVERFLOW = 1000;
    /**
     * internal map
     */
    private final Map<String, Long> map = new ConcurrentHashMap<>();
    /**
     * Query count
     */
    private final AtomicInteger queryCount = new AtomicInteger();
    /**
     * memory write status
     */
    private final AtomicBoolean force = new AtomicBoolean();
    /**
     * memory mapped lock
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * cache file
     */
    private final File cache;
    /**
     * Link effective time
     */
    private final int effectiveTime;
    /**
     * Memory mapped file
     */
    private MappedByteBuffer buffer;

    public MapRepeatValidator(File cache, int effectiveTime) {
        this.cache = cache;
        this.effectiveTime = effectiveTime;
        log.debug("Map repeat validator is enable");
    }

    @Override
    public boolean isRepeated(long time, String sign) {
        // Clean up expired elements
        if (queryCount.getAndIncrement() >= QUERY_OVERFLOW) {
            this.clearExpiredData();
            queryCount.set(0);
        }
        // Save sign ID
        Long value = map.putIfAbsent(sign, time);
        // Save mapped data
        if (value == null) {
            lock.lock();
            try {
                buffer.putLong(time);
                buffer.put(HexUtil.decodeHex(sign));
                force.set(true);
            } finally {
                lock.unlock();
            }
        }
        return value != null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initByteBuffer();
        initSyncMappedThread();
        restoreMapData();
    }

    private void initByteBuffer() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(cache, "rw")) {
            try (FileChannel channel = raf.getChannel()) {
                this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, effectiveTime * 102400);
            }
        }
    }

    private void initSyncMappedThread() {
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "MappedByteBuffer Task");
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(() -> {
            if (force.get() && lock.tryLock()) {
                try {
                    buffer.force();
                    force.set(false);
                    log.debug("Flush disk file");
                } finally {
                    lock.unlock();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void restoreMapData() {
        while (buffer.hasRemaining()) {
            long time = buffer.getLong();
            int pos = buffer.position();
            // end pointer
            if (time == 0) {
                buffer.position(pos - (2 << 2));
                break;
            }
            // expired data
            if (this.isExpired(time, effectiveTime)) {
                buffer.position(pos + (2 << 3));
                continue;
            }
            byte[] signBytes = new byte[16];
            buffer.get(signBytes);
            // save data
            map.put(HexUtil.encodeHexStr(signBytes), time);
        }
        log.debug("Read {} pieces of data", map.size());
        clearExpiredData();
    }

    /**
     * Clean up expired data
     */
    public void clearExpiredData() {
        // clear map
        ByteBuffer cache = ByteBuffer.allocate(buffer.capacity());
        map.forEach((sign, time) -> {
            if (isExpired(time, effectiveTime)) {
                map.remove(sign);
            } else {
                cache.putLong(time);
                cache.put(HexUtil.decodeHex(sign));
            }
        });
        lock.lock();
        try {
            // clear buffer
            buffer.clear();
            buffer.put(cache.array());
            // 8 bit long + 16 bit digest
            buffer.position(map.size() * 24);
            // find vaild pointer
            force.set(true);
        } finally {
            lock.unlock();
        }
    }
}
