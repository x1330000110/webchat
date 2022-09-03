package com.socket.secure.filter.validator.impl;

import cn.hutool.core.util.HexUtil;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
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
 * Repeated request validator based on {@link ConcurrentHashMap}+{@link MappedByteBuffer} <br>
 * The underlying implementation depends on the NIO module,
 * and the concurrency capability will not be affected.
 * Internally contains periodic memory mapping area and device file synchronization tasks.
 * When the memory mapping area changes, the file information is modified synchronously.
 * However, there may be some problems:
 * when the request is successfully authenticated but not synchronized to the local file in time,
 * the server is passively shut down (terminated tasks or down) some data may be lost,
 * leading to the risk of replay attacks. <br>
 * When this interceptor is started, the cache directory will generate files for data storage.
 * The file size is calculated according to the valid time of the request,
 * The maximum number of accepted requests per second can be configured
 * {@link SecureProperties#getMaximumConcurrencyPerSecond()},
 * When the request exceeds the critical point,
 * it possible thrown {@link BufferOverflowException} exception.
 *
 * @see RedisRepeatValidator
 * @see ConcurrentHashMap
 * @see MappedByteBuffer
 */
public class MappedRepeatValidator implements RepeatValidator, InitializingBean {
    /**
     * Expired element cleanup threshold
     */
    private static final int QUERY_OVERFLOW = 1000;
    /**
     * Request data block size
     */
    private static final int BLOCK_SIZE = 24;
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
    private final int effective;
    /**
     * Maximum number of concurrent
     */
    private final int maximum;
    /**
     * Memory mapped file
     */
    private MappedByteBuffer buffer;

    public MappedRepeatValidator(File cache, int effective, int maximum) {
        this.cache = cache;
        this.maximum = maximum;
        this.effective = effective;
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
                this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, effective * maximum * BLOCK_SIZE);
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
            if (this.isExpired(time, effective)) {
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
    private void clearExpiredData() {
        // clear map
        ByteBuffer cache = ByteBuffer.allocate(buffer.capacity());
        map.forEach((sign, time) -> {
            if (isExpired(time, effective)) {
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
            buffer.position(map.size() * BLOCK_SIZE);
            // find vaild pointer
            force.set(true);
        } finally {
            lock.unlock();
        }
    }
}
