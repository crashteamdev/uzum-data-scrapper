package dev.crashteam.uzumdatascrapper.job.category;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import dev.crashteam.uzum.scrapper.data.v1.UzumCategoryChange;
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent;
import dev.crashteam.uzumdatascrapper.exception.CategoryRequestException;
import dev.crashteam.uzumdatascrapper.model.dto.UzumCategoryMessage;
import dev.crashteam.uzumdatascrapper.model.stream.AwsStreamMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumCategory;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
import dev.crashteam.uzumdatascrapper.service.integration.UzumService;
import dev.crashteam.uzumdatascrapper.service.stream.AwsStreamMessagePublisher;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
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
    AwsStreamMessagePublisher awsStreamMessagePublisher;

    ExecutorService jobExecutor = Executors.newWorkStealingPool(6);

    @Value("${app.stream.category.key}")
    public String streamKey;

    @Value("${app.stream.category.maxlen}")
    public Long maxlen;

    @Value("${app.stream.category.waitPending}")
    public Long waitPending;

    @Value("${app.aws-stream.uzum-stream.name}")
    public String awsStreamName;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            List<UzumCategory.Data> rootCategories = retryTemplate.execute((RetryCallback<List<UzumCategory.Data>, CategoryRequestException>) retryContext -> {
                var categoryData = uzumService.getRootCategories();
                if (categoryData == null) {
                    throw new CategoryRequestException();
                }
                return categoryData;
            });
            List<Callable<PutRecordsRequestEntry>> callables = new ArrayList<>();
            List<PutRecordsRequestEntry> entries = new ArrayList<>();
            UzumGQLResponse gqlResponse = uzumService.retryableGQLRequest(1, 0, 0);
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree = gqlResponse.getData().getMakeSearch().getCategoryTree();
            for (UzumCategory.Data rootCategory : rootCategories) {
                callables.add(postCategoryRecord(rootCategory, categoryTree));
            }

            try {
                jobExecutor.invokeAll(callables)
                        .forEach(it -> {
                            try {
                                entries.add(it.get());
                            } catch (Exception e) {
                                log.error("Error while trying to fill AWS entries:", e);
                            }
                        });
            } catch (Exception e) {
                log.error("Error on invoking callables for categories - {}",
                        Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(""), e);
            }

            try {
                PutRecordsResult recordsResult = awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName, entries));
                log.info("CATEGORY JOB : Posted [{}] records to AWS stream - [{}]",
                        recordsResult.getRecords().size(), awsStreamName);
            } catch (Exception e) {
                log.error("CATEGORY JOB : AWS ERROR, couldn't publish to stream - [{}]", awsStreamName, e);
            }

        } finally {
            jobExecutor.shutdown();
        }
        log.info("Finished category job");

    }

    @SneakyThrows
    private Callable<PutRecordsRequestEntry> postCategoryRecord(
            UzumCategory.Data rootCategory,
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree
    ) {
        return () -> getAwsMessage(rootCategory, categoryTree);
    }

    private dev.crashteam.uzum.scrapper.data.v1.UzumCategory mapToMessage(
            UzumCategory.Data category,
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree) {
        var categoryBuilder = dev.crashteam.uzum.scrapper.data.v1.UzumCategory.newBuilder()
                .setCategoryId(category.getId())
                .setIsAdult(category.isAdult())
                .setIsEco(category.isEco())
                .setTitle(category.getTitle());

        if (!CollectionUtils.isEmpty(category.getChildren())) {
            List<dev.crashteam.uzum.scrapper.data.v1.UzumCategory> childrenCategories = new ArrayList<>();
            for (UzumCategory.Data child : category.getChildren()) {
                childrenCategories.add(mapToMessage(child, categoryTree));
            }
            categoryBuilder.addAllChildren(childrenCategories);
        } else {
            if (hasChildren(category.getId(), categoryTree)) {
                List<dev.crashteam.uzum.scrapper.data.v1.UzumCategory> childrenCategories = new ArrayList<>();
                Set<UzumGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                        .filter(it -> it.getCategory().getParent() != null
                                && Objects.equals(it.getCategory().getParent().getId(), category.getId()))
                        .collect(Collectors.toSet());
                for (UzumGQLResponse.ResponseCategoryWrapper responseCategory : responseCategories) {
                    childrenCategories.add(mapChildrenCategory(responseCategory, categoryTree, category.isEco()));
                }
                categoryBuilder.addAllChildren(childrenCategories);
            }
        }

        return categoryBuilder.build();
    }

    private dev.crashteam.uzum.scrapper.data.v1.UzumCategory mapChildrenCategory(
            UzumGQLResponse.ResponseCategoryWrapper responseCategory,
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree,
            boolean isEco) {
        UzumGQLResponse.ResponseCategory childCategory = responseCategory.getCategory();
        var categoryBuilder = dev.crashteam.uzum.scrapper.data.v1.UzumCategory.newBuilder()
                .setCategoryId(childCategory.getId())
                .setIsAdult(childCategory.isAdult())
                .setIsEco(isEco)
                .setTitle(childCategory.getTitle());
        if (hasChildren(childCategory.getId(), categoryTree)) {
            List<dev.crashteam.uzum.scrapper.data.v1.UzumCategory> childrenCategories = new ArrayList<>();
            Set<UzumGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), childCategory.getId()))
                    .collect(Collectors.toSet());
            for (UzumGQLResponse.ResponseCategoryWrapper category : responseCategories) {
                childrenCategories.add(mapChildrenCategory(category, categoryTree, isEco));
            }
            categoryBuilder.addAllChildren(childrenCategories);
        }

        return categoryBuilder.build();
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

    private PutRecordsRequestEntry getAwsMessage(
            UzumCategory.Data category,
            List<UzumGQLResponse.ResponseCategoryWrapper> categoryTree) {
        try {
            Instant now = Instant.now();
            dev.crashteam.uzum.scrapper.data.v1.UzumCategory uzumCategory = mapToMessage(category, categoryTree);
            UzumScrapperEvent scrapperEvent = UzumScrapperEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setScrapTime(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .setEventPayload(UzumScrapperEvent.EventPayload.newBuilder()
                            .setUzumCategoryChange(UzumCategoryChange.newBuilder()
                                    .setCategory(uzumCategory).build())
                            .build())
                    .build();
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setPartitionKey(String.valueOf(category.getId()));
            requestEntry.setData(ByteBuffer.wrap(scrapperEvent.toByteArray()));
            return requestEntry;
        } catch (Exception ex) {
            log.error("Unexpected exception during publish AWS stream message returning NULL", ex);
        }
        return null;
    }
}
