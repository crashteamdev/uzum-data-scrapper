package dev.crashteam.uzumdatascrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.CategoryRequestException;
import dev.crashteam.uzumdatascrapper.mapper.UzumCategoryToMessageMapper;
import dev.crashteam.uzumdatascrapper.model.dto.UzumCategoryMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumCategory;
import dev.crashteam.uzumdatascrapper.service.integration.UzumService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
public class CategoryJob implements Job {

    @Autowired
    UzumService uzumService;

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${app.stream.category.key}")
    public String streamKey;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        List<UzumCategory.Data> data = retryTemplate.execute((RetryCallback<List<UzumCategory.Data>, CategoryRequestException>) retryContext -> {
            var categoryData = uzumService.getRootCategories();
            if (categoryData == null) {
                throw new CategoryRequestException();
            }
            return categoryData;
        });
        for (UzumCategory.Data dataChild : data) {
            postCategoryRecord(dataChild);
        }

    }

    @SneakyThrows
    private void postCategoryRecord(UzumCategory.Data data) {
        UzumCategoryMessage categoryMessage = UzumCategoryToMessageMapper.categoryToMessage(data);
        RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                        objectMapper.writeValueAsBytes(categoryMessage)));
        log.info("Posted [stream={}] category record with id - [{}]",
                streamKey, recordId);
        for (UzumCategory.Data child : data.getChildren()) {
            postCategoryRecord(child);
        }
    }
}
