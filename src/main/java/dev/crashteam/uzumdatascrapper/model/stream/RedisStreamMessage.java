package dev.crashteam.uzumdatascrapper.model.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedisStreamMessage extends Message<Object> {
    private long maxLen;
    private String messageKey;
    private Long waitPending;

    public RedisStreamMessage(String streamKey, Object message, long maxLen, String messageKey, Long waitPending) {
        super(streamKey, message);
        this.maxLen = maxLen;
        this.waitPending = waitPending;
        this.messageKey = messageKey;
    }
}
