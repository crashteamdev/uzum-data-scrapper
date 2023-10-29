package dev.crashteam.uzumdatascrapper.service;

import dev.crashteam.uzumdatascrapper.exception.UzumGqlRequestException;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;
import dev.crashteam.uzumdatascrapper.service.integration.UzumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobUtilService {

    private final UzumService uzumService;
    private final RetryTemplate retryTemplate;

    public UzumProduct.ProductData getProductData(Long itemId) {
        return retryTemplate.execute((RetryCallback<UzumProduct.ProductData, UzumGqlRequestException>) retryContext -> {
            UzumProduct product = uzumService.getProduct(itemId);
            if (!CollectionUtils.isEmpty(product.getErrors())) {
                String errorMessage = product.getErrors()
                        .stream()
                        .map(UzumProduct.ProductError::getDetailMessage)
                        .findFirst()
                        .orElse("");
                throw new UzumGqlRequestException("Get product with id - %s failed with message - %s"
                        .formatted(itemId, errorMessage));
            }
            return Optional.ofNullable(product.getPayload()).map(UzumProduct.Payload::getData)
                    .orElseThrow(() -> new UzumGqlRequestException("Product catalog can't be null"));
        });
    }

    public UzumGQLResponse getResponse(JobExecutionContext jobExecutionContext, AtomicLong offset, Long categoryId, Long limit) {
        return retryTemplate.execute((RetryCallback<UzumGQLResponse, UzumGqlRequestException>) retryContext -> {
            UzumGQLResponse response = uzumService.getGQLSearchResponse(String.valueOf(categoryId), offset.get(), limit);
            if (!CollectionUtils.isEmpty(response.getErrors())) {
                for (UzumGQLResponse.GQLError error : response.getErrors()) {
                    if (error.getMessage().contains("offset")) {
                        log.warn("Finished collecting data for id - {}, " +
                                "because of response error object with message - {}", categoryId, error.getMessage());
                        return null;
                    } else if (error.getMessage().contains("429")) {
                        log.warn("Got 429 http status from request for category id {}", categoryId);
                        throw new UzumGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                    } else {
                        offset.addAndGet(limit);
                        jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                        throw new UzumGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                    }
                }
            }
            return response;
        });
    }
}
