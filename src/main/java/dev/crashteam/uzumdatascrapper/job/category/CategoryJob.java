package dev.crashteam.uzumdatascrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.CategoryRequestException;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    ExecutorService jobExecutor = Executors.newWorkStealingPool(6);

    @Value("${app.stream.category.key}")
    public String streamKey;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            List<UzumCategory.Data> rootCategories = retryTemplate.execute((RetryCallback<List<UzumCategory.Data>, CategoryRequestException>) retryContext -> {
                var categoryData = uzumService.getRootCategories();
                if (categoryData == null) {
                    throw new CategoryRequestException();
                }
                return categoryData;
            });
            List<Callable<Void>> callables = new ArrayList<>();
            for (UzumCategory.Data rootCategory : rootCategories) {
                callables.add(postCategoryRecord(rootCategory));
            }
            jobExecutor.invokeAll(callables);
        } finally {
            jobExecutor.shutdown();
        }

    }

    @SneakyThrows
    private Callable<Void> postCategoryRecord(UzumCategory.Data rootCategory) {
        return () -> {
            UzumCategoryMessage categoryMessage = categoryToMessage(rootCategory);
            RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                    Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                            objectMapper.writeValueAsBytes(categoryMessage)));
            log.info("Posted [stream={}] category record with id - [{}]",
                    streamKey, recordId);
            return null;
        };
    }

    private UzumCategoryMessage categoryToMessage(UzumCategory.Data category) {
        UzumCategoryMessage categoryMessage = UzumCategoryMessage.builder()
                .id(category.getId())
                .adult(category.isAdult())
                .eco(category.isEco())
                .title(category.getTitle())
                .time(Instant.now().toEpochMilli())
                .build();
        if (!CollectionUtils.isEmpty(category.getChildren())) {
            List<UzumCategoryMessage> childrenCategories = new ArrayList<>();
            for (UzumCategory.Data child : category.getChildren()) {
                childrenCategories.add(categoryToMessage(child));
            }
            categoryMessage.setChildren(childrenCategories);
        } else {
            List<UzumCategoryMessage> childrenCategories = new ArrayList<>();
            UzumGQLResponse categories = uzumService.retryableGQLRequest(category.getId(), 0, 0);
            categories.getData().getMakeSearch().getCategoryTree()
                    .stream()
                    .filter(it -> it.getCategory().getParent() != null)
                    .filter(it -> it.getCategory().getParent().getId() == category.getId())
                    .forEach(it -> {
                        UzumGQLResponse.ResponseCategory responseCategory = it.getCategory();
                        UzumCategoryMessage childCategoryMessage = UzumCategoryMessage.builder()
                                .id(responseCategory.getId())
                                .adult(responseCategory.isAdult())
                                .eco(category.isEco())
                                .title(responseCategory.getTitle())
                                .time(Instant.now().toEpochMilli())
                                .build();
                        childrenCategories.add(childCategoryMessage);
                    });
            categoryMessage.setChildren(childrenCategories);
        }
        return categoryMessage;
    }
}
