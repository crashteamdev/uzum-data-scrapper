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
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

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
        UzumCategory.Data data = retryTemplate.execute((RetryCallback<UzumCategory.Data, CategoryRequestException>) retryContext -> {
            var categoryData = uzumService.getCategoryData(1L);
            if (categoryData.getPayload() == null || !CollectionUtils.isEmpty(categoryData.getErrors())) {
                throw new CategoryRequestException();
            }
            return categoryData.getPayload().getCategory();
        });
        for (UzumCategory.Data dataChild : data.getChildren()) {
            UzumCategoryMessage categoryMessage = UzumCategoryToMessageMapper.categoryToMessage(dataChild);
            RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                    Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                            objectMapper.writeValueAsBytes(categoryMessage)));
            log.info("Posted [stream={}] category record with id - [{}]",
                    streamKey, recordId);
        }

    }
}
