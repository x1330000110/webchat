package com.socket.secure.filter.validator;

import com.socket.secure.filter.validator.impl.MapRepeatValidator;
import com.socket.secure.filter.validator.impl.RedisRepeatValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repeated request validator
 *
 * @date 2021.8.2
 * @see MapRepeatValidator
 * @see RedisRepeatValidator
 */
public interface RepeatValidator {
    Logger log = LoggerFactory.getLogger(RepeatValidator.class);

    /**
     * Verify this request is expired
     *
     * @param timestamp     request timestamp (unit: millisecond)
     * @param effectiveTime effectiveTime (unit: second)
     * @return link expired return true
     */
    default boolean isExpired(long timestamp, long effectiveTime) {
        return (System.currentTimeMillis() - timestamp) / 1000 > effectiveTime;
    }

    /**
     * Verify this request is repeated
     *
     * @param timestamp request timestamp
     * @param sign      request link signature
     * @return link repeated return true
     */
    boolean isRepeated(long timestamp, String sign);
}
