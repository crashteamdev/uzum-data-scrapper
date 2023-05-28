package dev.crashteam.uzumdatascrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.model.stream.RedisStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamMessagePublisher implements MessagePublisher<RedisStreamMessage> {

    private final RedisStreamCommands streamCommands;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public RecordId publish(RedisStreamMessage message) {
        if (message.getWaitPending() != null && message.getWaitPending() > 0) {
            StreamInfo.XInfoGroups streamInfo = getStreamInfo(message.getTopic());
            if (streamInfo != null && streamInfo.stream()
                    .anyMatch(it -> {
                        boolean wait = it.pendingCount() >= message.getWaitPending();
                        if (wait) {
                            log.warn("Pending messages are above [{}], currently pending - [{}], waiting for consumer to finish...",
                                    message.getWaitPending(), it.pendingCount());
                        }
                        return wait;
                    })) {
                Thread.sleep(5000L);
                return publish(message);
            }
        }
        return streamCommands.xAdd(MapRecord.create(message.getTopic().getBytes(StandardCharsets.UTF_8),
                Collections.singletonMap(message.getMessageKey().getBytes(StandardCharsets.UTF_8),
                        objectMapper.writeValueAsBytes(message.getMessage()))), RedisStreamCommands.XAddOptions.maxlen(message.getMaxLen()));

    }

    private StreamInfo.XInfoGroups getStreamInfo(String streamKey) {
        try {
            return streamCommands.xInfoGroups(streamKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Stream info with key {} not found", streamKey);
        }
        return null;
    }
}
