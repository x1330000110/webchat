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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repeat request validator based on {@link ConcurrentHashMap}+{@link MappedByteBuffer}<br>
 * Internally contains memory mapping area and periodic file synchronization tasks.
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
 * its possible thrown {@link BufferOverflowException} exception.
 *
 * @see ConcurrentHashMap
 * @see MappedByteBuffer
 */
public class MappedRepeatValidator implements RepeatValidator, InitializingBean {
    /**
     * Expired element cleanup threshold
     */
    private static final int QUERY_OVERFLOW = 1000;
    /**
     * Request data block size（8 bit long + 16 bit digest）
     */
    private static final int BLOCK_SIZE = 24;
    /**
     * internal map
     */
    private final Map<String, Long> internalMap = new ConcurrentHashMap<>();
    /**
     * Query count
     */
    private final AtomicInteger queryCount = new AtomicInteger();
    /**
     * Disk refresh wait status
     */
    private final Object force = new Object();
    /**
     * memory mapped lock
     */
    private final Object write = new Object();
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

    public MappedRepeatValidator(File cache, SecureProperties properties) {
        this.cache = cache;
        this.effective = properties.getLinkValidTime();
        this.maximum = properties.getMaximumConcurrencyPerSecond();
        log.debug("Mapped repeat validator is enable");
    }

    @Override
    public boolean isRepeated(long time, String sign) {
        // Clean up expired elements
        if (queryCount.getAndIncrement() >= QUERY_OVERFLOW) {
            this.clearExpiredData();
            queryCount.set(0);
        }
        // Save sign ID
        Long value = internalMap.putIfAbsent(sign, time);
        // Save mapped data
        if (value == null) {
            this.writeBuffer(buffer, time, sign);
        }
        return value != null;
    }

    /**
     * Clean up expired data
     */
    private void clearExpiredData() {
        // clear map
        ByteBuffer cache = ByteBuffer.allocate(buffer.capacity());
        internalMap.forEach((sign, time) -> {
            if (this.isExpired(time, effective)) {
                internalMap.remove(sign);
            } else {
                this.writeBuffer(cache, time, sign);
            }
        });
        // clear buffer
        synchronized (write) {
            buffer.clear();
            buffer.put(cache.array());
            buffer.position(internalMap.size() * BLOCK_SIZE);
        }
    }

    /**
     * write to buffer
     */
    private void writeBuffer(ByteBuffer buffer, long time, String sign) {
        // write buffer
        synchronized (write) {
            buffer.putLong(time);
            buffer.put(HexUtil.decodeHex(sign));
        }
        // refresh disk
        synchronized (force) {
            force.notify();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initByteBuffer();
        this.startFileBufferSyncThread();
    }

    /**
     * Restore Map data
     */
    private void initByteBuffer() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(cache, "rw")) {
            try (FileChannel channel = raf.getChannel()) {
                int limit = effective / 1000 * maximum * BLOCK_SIZE;
                this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, limit);
            }
        }
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
            internalMap.put(HexUtil.encodeHexStr(signBytes), time);
        }
        log.debug("Read {} pieces of data", internalMap.size());
        this.clearExpiredData();
    }

    /**
     * Initialize Memory Timing Mapped File Task
     */
    private void startFileBufferSyncThread() {
        Thread thread = new Thread(() -> {
            while (true) {
                synchronized (force) {
                    try {
                        force.wait();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                    synchronized (write) {
                        buffer.force();
                        log.debug("Flush disk file");
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
