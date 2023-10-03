package dev.crashteam.uzumdatascrapper.aws;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AwsStreamClient {

    private final AmazonKinesis amazonKinesis;

    public PutRecordsResult sendMessage(String streamName, List<PutRecordsRequestEntry> entries) {
        PutRecordsRequest createRecordsRequest = new PutRecordsRequest();
        createRecordsRequest.setStreamName(streamName);
        createRecordsRequest.setRecords(entries);
        return amazonKinesis.putRecords(createRecordsRequest);
    }
}
