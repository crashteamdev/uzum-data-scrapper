package dev.crashteam.uzumdatascrapper.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

@Component
public class AwsStreamClient {

    private final KinesisProducerConfiguration producerConfig;

    public AwsStreamClient(
            @Value("${app.aws-stream.endpoint}") String endpoint,
            @Value("${app.aws-stream.accessKey}") String accessKey,
            @Value("${app.aws-stream.secretKey}") String secretKey,
            @Value("${app.aws-stream.region}") String region) {
        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        producerConfig = new KinesisProducerConfiguration()
                .setKinesisEndpoint(endpoint)
                .setCredentialsProvider(new AWSStaticCredentialsProvider(awsCredentials))
                .setVerifyCertificate(false)
                .setRegion(region);
    }

    public UserRecordResult sendMessage(
            String streamName,
            String partitionKey,
            byte[] message
    ) throws ExecutionException, InterruptedException {
        KinesisProducer kinesisProducer = new KinesisProducer(producerConfig);
        return kinesisProducer.addUserRecord(
                streamName,
                partitionKey,
                ByteBuffer.wrap(message)
        ).get();
    }
}
