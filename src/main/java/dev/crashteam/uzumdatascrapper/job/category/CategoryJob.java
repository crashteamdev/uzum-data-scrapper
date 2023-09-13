package dev.crashteam.uzumdatascrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.CategoryRequestException;
import dev.crashteam.uzumdatascrapper.model.dto.UzumCategoryMessage;
import dev.crashteam.uzumdatascrapper.model.stream.RedisStreamMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumCategory;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
import dev.crashteam.uzumdatascrapper.service.RedisStreamMessagePublisher;
import dev.crashteam.uzumdatascrapper.service.integration.UzumService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@DisallowConcurrentExecution
public class CategoryJob implements Job {

    @Autowired
    UzumService uzumService;

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisStreamMessagePublisher messagePublisher;

    ExecutorService jobExecutor = Executors.newWorkStealingPool(6);

    @Value("${app.stream.category.key}")
    public String streamKey;

    @Value("${app.stream.category.maxlen}")
    public Long maxlen;

    @Value("${app.stream.category.waitPending}")
    public Long waitPending;

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
            UzumGQLResponse gqlResponse = uzumService.retryableGQLRequest(1, 0, 0);
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree = gqlResponse.getData().getMakeSearch().getCategoryTree();
            for (UzumCategory.Data rootCategory : rootCategories) {
                callables.add(postCategoryRecord(rootCategory, categoryTree));
            }
            jobExecutor.invokeAll(callables);
        } finally {
            jobExecutor.shutdown();
        }
        log.info("Finished category job");

    }

    @SneakyThrows
    private Callable<Void> postCategoryRecord(UzumCategory.Data rootCategory, List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree) {
        return () -> {
            UzumCategoryMessage categoryMessage = categoryToMessage(rootCategory, categoryTree);
            RecordId recordId = messagePublisher.publish(new RedisStreamMessage(streamKey, categoryMessage, maxlen,
                    "category", waitPending));
            log.info("Posted [stream={}] category record with record_id - [{}] and category_id - [{}]",
                    streamKey, recordId, rootCategory.getId());
            return null;
        };
    }

    private UzumCategoryMessage categoryToMessage(UzumCategory.Data category, List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree) {
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
                childrenCategories.add(categoryToMessage(child, categoryTree));
            }
            categoryMessage.setChildren(childrenCategories);
        } else {
            if (hasChildren(category.getId(), categoryTree)) {
                List<UzumCategoryMessage> childrenCategories = new ArrayList<>();
                Set<UzumGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                        .filter(it -> it.getCategory().getParent() != null
                                && Objects.equals(it.getCategory().getParent().getId(), category.getId()))
                        .collect(Collectors.toSet());
                for (UzumGQLResponse.ResponseCategoryWrapper responseCategory : responseCategories) {
                    childrenCategories.add(getChildrenCategory(responseCategory, categoryTree, category.isEco()));
                }
                categoryMessage.setChildren(childrenCategories);
            }
        }

        return categoryMessage;
    }

    private UzumCategoryMessage getChildrenCategory(UzumGQLResponse.ResponseCategoryWrapper responseCategory,
                                                    List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree, boolean isEco) {

        UzumGQLResponse.ResponseCategory childCategory = responseCategory.getCategory();
        UzumCategoryMessage childCategoryMessage = UzumCategoryMessage.builder()
                .id(childCategory.getId())
                .adult(childCategory.isAdult())
                .eco(isEco)
                .title(childCategory.getTitle())
                .time(Instant.now().toEpochMilli())
                .build();
        if (hasChildren(childCategory.getId(), categoryTree)) {
            List<UzumCategoryMessage> childrenCategories = new ArrayList<>();
            Set<UzumGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), childCategory.getId()))
                    .collect(Collectors.toSet());
            for (UzumGQLResponse.ResponseCategoryWrapper category : responseCategories) {
                childrenCategories.add(getChildrenCategory(category, categoryTree, isEco));
            }
            childCategoryMessage.setChildren(childrenCategories);
        }

        return childCategoryMessage;
    }

    private boolean hasChildren(Long categoryId, List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree) {
        return categoryTree.stream().anyMatch(it -> it.getCategory().getParent() != null
                && Objects.equals(it.getCategory().getParent().getId(), categoryId));

    }
}
