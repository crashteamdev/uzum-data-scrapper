package dev.crashteam.uzumdatascrapper.job.position;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.UzumGqlRequestException;
import dev.crashteam.uzumdatascrapper.model.Constant;
import dev.crashteam.uzumdatascrapper.model.dto.ProductPositionMessage;
import dev.crashteam.uzumdatascrapper.model.stream.RedisStreamMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;
import dev.crashteam.uzumdatascrapper.service.JobUtilService;
import dev.crashteam.uzumdatascrapper.service.RedisStreamMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PositionJob implements Job {

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisStreamMessagePublisher messagePublisher;

    @Value("${app.stream.position.key}")
    public String streamKey;

    @Value("${app.stream.position.maxlen}")
    public Long maxlen;

    @Value("${app.stream.position.waitPending}")
    public Long waitPending;

    ExecutorService jobExecutor = Executors.newWorkStealingPool(3);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Instant start = Instant.now();
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = Long.valueOf(jobDetail.getJobDataMap().get(Constant.POSITION_CATEGORY_KEY).toString());
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        jobDetail.getJobDataMap().put("totalItemProcessed", new AtomicLong(0));
        log.info("Starting position job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        AtomicLong totalItemProcessed = (AtomicLong) jobDetail.getJobDataMap().get("totalItemProcessed");
        long limit = 100;
        AtomicLong position = new AtomicLong(0);
        try {
            while (true) {
                try {
                    UzumGQLResponse gqlResponse = jobUtilService.getResponse(jobExecutionContext, offset, categoryId, limit);
                    if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                        break;
                    }
                    if (gqlResponse.getData().getMakeSearch().getTotal() <= totalItemProcessed.get()) {
                        log.info("Total GQL response items - [{}] less or equal than total processed items - [{}] of category - [{}], " +
                                "skipping further parsing... ", gqlResponse.getData().getMakeSearch().getTotal(), totalItemProcessed.get(), categoryId);
                        break;
                    }
                    var productItems = Optional.ofNullable(gqlResponse.getData()
                            .getMakeSearch())
                            .map(UzumGQLResponse.MakeSearch::getItems)
                            .filter(it -> !CollectionUtils.isEmpty(it))
                            .orElse(Collections.emptyList());
                    if (CollectionUtils.isEmpty(productItems)) {
                        log.warn("Skipping position job gql request for categoryId - {} with offset - {}, cause items are empty", categoryId, offset);
                        offset.addAndGet(limit);
                        jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                        continue;
                    }
                    log.info("Iterate through products for position itemsCount={};categoryId={}", productItems.size(), categoryId);
                    List<Callable<Void>> callables = new ArrayList<>();
                    for (UzumGQLResponse.CatalogCardWrapper productItem : productItems) {
                        callables.add(postPositionRecord(productItem, position, categoryId));
                    }
                    jobExecutor.invokeAll(callables);
                    offset.addAndGet(limit);
                    totalItemProcessed.addAndGet(productItems.size());
                    jobExecutionContext.getJobDetail().getJobDataMap().put("totalItemProcessed", totalItemProcessed);
                    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                } catch (Exception e) {
                    log.error("Search for position with category id [{}] finished with exception - [{}]", categoryId,
                            Optional.ofNullable(e.getCause()).orElse(e).getMessage());
                    break;
                }
            }
        } finally {
            jobExecutor.shutdown();
        }
        Instant end = Instant.now();
        log.info("Position job - Finished collecting for category id - {}, in {} seconds", categoryId,
                Duration.between(start, end).toSeconds());
    }

    private Callable<Void> postPositionRecord(UzumGQLResponse.CatalogCardWrapper productItem, AtomicLong position, Long categoryId) {
        return () -> {
            position.incrementAndGet();
            Long itemId = Optional.ofNullable(productItem.getCatalogCard()).map(UzumGQLResponse.CatalogCard::getProductId)
                    .orElseThrow(() -> new UzumGqlRequestException("Catalog card can't be null"));
            UzumGQLResponse.CatalogCard productItemCard = productItem.getCatalogCard();
            List<UzumGQLResponse.CharacteristicValue> productItemCardCharacteristics = productItemCard.getCharacteristicValues();
            UzumProduct.ProductData productResponse = jobUtilService.getProductData(itemId);
            if (productResponse == null) {
                log.info("Product data with id - %s returned null, continue with next item, if it exists...".formatted(itemId));
                return null;
            }
            if (!CollectionUtils.isEmpty(productItemCardCharacteristics)) {
                var productItemCardCharacteristic = productItemCardCharacteristics.get(0);
                var characteristicId = productItemCardCharacteristic.getId();
                var productCharacteristics = productResponse.getCharacteristics();

                Integer indexOfCharacteristic = null;
                CHARACTERISTICS:
                for (UzumProduct.CharacteristicsData productCharacteristic : productCharacteristics) {
                    List<UzumProduct.Characteristic> characteristicValues = productCharacteristic.getValues();
                    if (CollectionUtils.isEmpty(characteristicValues)) continue;
                    for (int index = 0; index < characteristicValues.size(); index++) {
                        UzumProduct.Characteristic characteristic = characteristicValues.get(index);
                        if (characteristic != null && characteristic.getId().equals(characteristicId)) {
                            indexOfCharacteristic = index;
                            break CHARACTERISTICS;
                        }
                    }
                }

                if (indexOfCharacteristic == null) {
                    log.warn("Something goes wrong. Can't find index of characteristic." +
                            " productId={}; characteristicId={}", productItemCard.getProductId(), characteristicId);
                    return null;
                }
                Integer finalIndexOfCharacteristic = indexOfCharacteristic;
                List<Long> skuIds = productResponse.getSkuList()
                        .stream()
                        .filter(productSku -> {
                            if (!CollectionUtils.isEmpty(productSku.getCharacteristics())) {
                                boolean anyMatch = productSku.getCharacteristics().stream()
                                        .anyMatch(it -> finalIndexOfCharacteristic.equals(it.getValueIndex()));
                                return anyMatch && productSku.getAvailableAmount() > 0;
                            }
                            return false;
                        }).map(UzumProduct.SkuData::getId).toList();
                for (Long skuId : skuIds) {
                    ProductPositionMessage positionMessage = ProductPositionMessage.builder()
                            .position(position.get())
                            .productId(productItemCard.getProductId())
                            .skuId(skuId)
                            .categoryId(categoryId)
                            .time(Instant.now().toEpochMilli())
                            .build();
                    RecordId recordId = messagePublisher.publish(new RedisStreamMessage(streamKey, positionMessage, maxlen,
                            "position", waitPending));
                    log.info("Posted [stream={}] position record with id - [{}] for category id - [{}], product id - [{}], sku id - [{}]",
                            streamKey, recordId, categoryId, productItemCard.getProductId(), skuId);
                }
            } else {
                List<Long> skuIds = productResponse.getSkuList()
                        .stream()
                        .filter(sku -> sku.getAvailableAmount() > 0)
                        .map(UzumProduct.SkuData::getId)
                        .toList();

                for (Long skuId : skuIds) {
                    ProductPositionMessage positionMessage = ProductPositionMessage.builder()
                            .position(position.get())
                            .productId(productItemCard.getProductId())
                            .skuId(skuId)
                            .categoryId(categoryId)
                            .time(Instant.now().toEpochMilli())
                            .build();
                    RecordId recordId = messagePublisher.publish(new RedisStreamMessage(streamKey, positionMessage, maxlen,
                            "position", waitPending));
                    log.info("Posted [stream={}] position record with id - [{}], for category id - [{}], product id - [{}], sku id - [{}]",
                            streamKey, recordId, categoryId, productItemCard.getProductId(), skuId);
                }
            }
            return null;
        };
    }
}
