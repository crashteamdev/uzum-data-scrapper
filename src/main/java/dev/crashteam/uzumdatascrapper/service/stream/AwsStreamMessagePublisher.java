package dev.crashteam.uzumdatascrapper.service.stream;

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
    public Object publish(AwsStreamMessage message) {
        return awsStreamClient.sendMessage(
                message.getTopic(),
                message.getPartitionKey(),
                message.getMessage().toByteArray()
        );
    }
}
