package dev.crashteam.uzumdatascrapper.service.stream;

import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.uzumdatascrapper.aws.AwsStreamClient;
import dev.crashteam.uzumdatascrapper.model.stream.AwsStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AwsStreamMessagePublisher implements MessagePublisher<AwsStreamMessage> {

    private final AwsStreamClient awsStreamClient;

    @SneakyThrows
    @Override
    public PutRecordsResult publish(AwsStreamMessage message) {
        return awsStreamClient.sendMessage(
                message.getTopic(),
                message.getMessage()
        );
    }
}
