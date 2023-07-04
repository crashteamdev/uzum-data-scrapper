package dev.crashteam.uzumdatascrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.CategoryRequestException;
import dev.crashteam.uzumdatascrapper.mapper.UzumCategoryToMessageMapper;
import dev.crashteam.uzumdatascrapper.model.dto.UzumCategoryMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumCategory;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
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
        List<UzumCategory.Data> rootCategories = retryTemplate.execute((RetryCallback<List<UzumCategory.Data>, CategoryRequestException>) retryContext -> {
            var categoryData = uzumService.getRootCategories();
            if (categoryData == null) {
                throw new CategoryRequestException();
            }
            return categoryData;
        });
        for (UzumCategory.Data childCategories : rootCategories) {
            postCategoryRecord(childCategories);
        }

    }

    @SneakyThrows
    private void postCategoryRecord(UzumCategory.Data childCategory) {
        UzumCategoryMessage categoryMessage = UzumCategoryToMessageMapper.categoryToMessage(childCategory);
        RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                        objectMapper.writeValueAsBytes(categoryMessage)));
        log.info("Posted [stream={}] category record with id - [{}]",
                streamKey, recordId);
        for (UzumCategory.Data category : childCategory.getChildren()) {
            if (!CollectionUtils.isEmpty(category.getChildren())) {
                postCategoryRecord(category);
            } else {
                UzumGQLResponse categories = uzumService.retryableGQLRequest(category.getId(), 0, 0);
                categories.getData().getMakeSearch().getCategoryTree()
                        .stream()
                        .filter(it -> it.getCategory().getParent().getId() == category.getId())
                        .forEach(it -> {
                            try {
                                UzumCategoryMessage categoryGqlMessage = UzumCategoryToMessageMapper
                                        .categoryToMessage(it.getCategory(), category.isEco());
                                RecordId recordGqlId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                                        Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                                                objectMapper.writeValueAsBytes(categoryGqlMessage)));
                                log.info("Posted [stream={}] category record with id - [{}]",
                                        streamKey, recordGqlId);
                            } catch (Exception e) {
                                log.error("GQL categories exception - ", e);
                            }
                        });
            }
        }
    }
}
