package dev.crashteam.uzumdatascrapper.model.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsStreamMessage extends Message<com.google.protobuf.Message> {

    private final String partitionKey;

    public AwsStreamMessage(String stream, String partitionKey, com.google.protobuf.Message message) {
        super(stream, message);
        this.partitionKey = partitionKey;
    }
}
