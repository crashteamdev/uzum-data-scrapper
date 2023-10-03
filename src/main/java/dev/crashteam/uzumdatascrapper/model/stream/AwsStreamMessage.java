package dev.crashteam.uzumdatascrapper.model.stream;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsStreamMessage extends Message<List<PutRecordsRequestEntry>> {

    public AwsStreamMessage(String stream, List<PutRecordsRequestEntry> entries) {
        super(stream, entries);
    }
}
