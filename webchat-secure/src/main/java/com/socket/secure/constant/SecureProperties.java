package com.socket.secure.constant;

import com.socket.secure.filter.validator.impl.MappedRepeatValidator;
import com.socket.secure.filter.validator.impl.RedisRepeatValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.BufferOverflowException;

/**
 * Security constants configuration mapping
 */
@Component
@ConfigurationProperties(prefix = "secure.request")
public class SecureProperties {
    /**
     * Disguise the number of synchronized public key files <br>
     * A moderate number can increase the resolution speed of the client,
     * and vice versa can increase the difficulty of cracking
     * (it's not recommend changing this configuration).
     */
    private int disguiseFilesCount = 25;
    /**
     * Effective link save time (unit: second)<br>
     * The general request time will not exceed one minute, but in special scenarios (such as transferring large files),
     * the valid time verification can be appropriately extended,
     * otherwise there may be a problem of interception of expired requests. <br>
     * <b>Note:</b> lower this value, restart the server after waiting at least original configured time,
     * otherwise there may be a risk of replay attacks. <br>
     *
     * @see RedisRepeatValidator
     * @see MappedRepeatValidator
     * @see <a href="https://www.geeksforgeeks.org/replay-attack/">replay attack</a>
     */
    private int linkValidTime = 60;
    /**
     * Temporary request link prefix saved in Redis <br>
     * Redis-key is expressed as a 64-bit hexadecimal string signature.
     * It is recommended to use "<b>:</b>" as the end character of the prefix identifier,
     * because most Redis managers use "<b>:</b>" as the directory separator <br>
     */
    private String redisKeyPrefix = "Request:";
    /**
     * whether timestamps for global requests are stored in milliseconds <br>
     * Turn off this configuration, the timestamp will be saved in seconds,
     * that is only one request can be accepted per second <br>
     */
    private boolean exactRequestTime = true;
    /**
     * Whether to verify the file signature in the requested data <br>
     * Closing this configuration will ignore the verification of file content in all secure API requests,
     * and the corresponding client should also skip the generation of file signatures
     */
    private boolean verifyFileSignature = true;
    /**
     * The maximum number of concurrent events per second. If this number is exceeded,
     * a {@link BufferOverflowException} exception may occur.
     * This configuration is only valid for {@link MappedRepeatValidator}
     */
    private int maximumConcurrencyPerSecond = 1000;
    /**
     * Always verify the IP address uniqueness of the request
     * (turning off this configuration may be at risk of man-in-the-middle attack)
     */
    private boolean verifyRequestRemote = true;

    public int getDisguiseFilesCount() {
        return disguiseFilesCount;
    }

    public void setDisguiseFilesCount(int count) {
        this.disguiseFilesCount = count;
    }

    /**
     * Get the current request configuration millisecond level time
     */
    public int getLinkValidTime() {
        return exactRequestTime ? linkValidTime * 1000 : linkValidTime / 1000 * 1000;
    }

    public void setLinkValidTime(int time) {
        this.linkValidTime = time;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String prefix) {
        this.redisKeyPrefix = prefix;
    }

    public void setExactRequestTime(boolean exact) {
        this.exactRequestTime = exact;
    }

    public boolean isVerifyFileSignature() {
        return verifyFileSignature;
    }

    public void setVerifyFileSignature(boolean verify) {
        this.verifyFileSignature = verify;
    }

    public int getMaximumConcurrencyPerSecond() {
        return maximumConcurrencyPerSecond;
    }

    public void setMaximumConcurrencyPerSecond(int scond) {
        this.maximumConcurrencyPerSecond = scond;
    }

    public boolean isVerifyRequestRemote() {
        return verifyRequestRemote;
    }

    public void setVerifyRequestRemote(boolean verifyRequestRemote) {
        this.verifyRequestRemote = verifyRequestRemote;
    }
}
