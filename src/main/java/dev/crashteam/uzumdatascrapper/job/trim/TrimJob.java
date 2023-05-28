package dev.crashteam.uzumdatascrapper.job.trim;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@DisallowConcurrentExecution
public class TrimJob implements Job {

    @Autowired
    RedisStreamCommands streamCommands;

    @Value("${app.stream.category.key}")
    private String streamCategoryKey;

    @Value("${app.stream.position.key}")
    private String streamPositionKey;

    @Value("${app.stream.product.key}")
    private String streamProductKey;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            StreamInfo.XInfoStream categoryStream = getStreamInfo(streamCategoryKey);
            StreamInfo.XInfoStream positionStream = getStreamInfo(streamPositionKey);
            StreamInfo.XInfoStream productStream = getStreamInfo(streamProductKey);
            if (categoryStream != null) {
                Long trim = streamCommands.xTrim(streamCategoryKey.getBytes(StandardCharsets.UTF_8), 0);
                log.info("Deleted - [{}] category messages", trim);
            }

            if (positionStream != null) {
                Long trim = streamCommands.xTrim(streamPositionKey.getBytes(StandardCharsets.UTF_8), 3000);
                log.info("Deleted - [{}] position messages", trim);
            }

            if (productStream != null) {
                Long trim = streamCommands.xTrim(streamProductKey.getBytes(StandardCharsets.UTF_8), 3000);
                log.info("Deleted - [{}] product messages", trim);
            }
        } catch (Exception e) {
            log.error("Trim job failed cause - {}",
                    Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()));
        }
    }

    private StreamInfo.XInfoStream getStreamInfo(String streamKey) {
        try {
            return streamCommands.xInfo(streamKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Stream info with key {} not found", streamKey);
        }
        return null;
    }
}
