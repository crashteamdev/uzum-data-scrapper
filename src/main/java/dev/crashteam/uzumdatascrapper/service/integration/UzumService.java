package dev.crashteam.uzumdatascrapper.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.uzumdatascrapper.exception.UzumGqlRequestException;
import dev.crashteam.uzumdatascrapper.model.ProxyRequestParams;
import dev.crashteam.uzumdatascrapper.model.StyxProxyResult;
import dev.crashteam.uzumdatascrapper.model.uzum.*;
import dev.crashteam.uzumdatascrapper.service.integration.interceptor.AuthHeaderRequestService;
import dev.crashteam.uzumdatascrapper.util.RandomUserAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class UzumService {

    private final StyxProxyService proxyService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RetryTemplate retryTemplate;

    @Value("${app.integration.uzum.token}")
    private String authToken;

    @Value("${app.integration.timeout.from}")
    private Long fromTimeout;

    @Value("${app.integration.timeout.to}")
    private Long timeout;

    private static final String ROOT_URL = "https://api.umarket.uz/api";

    public List<UzumCategory.Data> getRootCategories() {
        try {
            return retryTemplate.execute((RetryCallback<List<UzumCategory.Data>, Exception>) retryContext -> {
                ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                        .key("headers")
                        .value(Map.of("Authorization", authToken,
                                "Accept-Language", "ru-RU")).build();
                Random randomTimeout = new Random();
                ProxyRequestParams requestParams = ProxyRequestParams.builder()
                        .url(ROOT_URL + "/main/root-categories?eco=false")
                        .httpMethod(HttpMethod.GET.name())
                        .context(Collections.singletonList(headers))
                        .build();
                Thread.sleep(randomTimeout.nextLong(fromTimeout, timeout));
                StyxProxyResult<UzumCategory> proxyResult = proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<>() {
                });
                return proxyResult.getBody().getPayload();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UzumProduct getProduct(Long id) {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "Accept-Language", "ru-RU",
                        "x-iid", "random_uuid()")).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .url(ROOT_URL + "/v2/product/%s".formatted(id))
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        try {
            Thread.sleep(randomTimeout.nextLong(fromTimeout, timeout));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<UzumProduct>>() {
        }).getBody();
    }

    public UzumCategoryChild getCategoryData(Long id) {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "x-iid", "random_uuid()",
                        "Accept-Language", "ru-RU")).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .url(ROOT_URL + "/category/v2/%s".formatted(id))
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        try {
            Thread.sleep(randomTimeout.nextLong(fromTimeout, timeout));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<UzumCategoryChild>>() {
        }).getBody();
    }

    public Set<Long> getIds() {
        log.info("Collecting category id's...");
        Set<Long> ids = new CopyOnWriteArraySet<>();
        List<Callable<Void>> callables = new ArrayList<>();
        for (UzumCategory.Data data : getRootCategories()) {
            callables.add(extractIdsAsync(data, ids));
        }
        List<Future<Void>> futures = callables.stream()
                .map(taskExecutor::submit)
                .toList();
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Exception while getting id's", e);
            }
        }
        log.info("Collected id's size - {}", ids.size());
        return ids;
    }

    public UzumGQLResponse getGQLSearchResponse(String categoryId, long offset, long limit) {
        log.info("Starting gql catalog search with values: [categoryId - {}] , [offset - {}], [limit - {}]"
                , categoryId, offset, limit);
        String query = "query getMakeSearch($queryInput: MakeSearchQueryInput!) {\n  makeSearch(query: $queryInput) {\n    id\n    queryId\n    queryText\n    category {\n      ...CategoryShortFragment\n      __typename\n    }\n    categoryTree {\n      category {\n        ...CategoryFragment\n        __typename\n      }\n      total\n      __typename\n    }\n    items {\n      catalogCard {\n        __typename\n        ...SkuGroupCardFragment\n      }\n      __typename\n    }\n    facets {\n      ...FacetFragment\n      __typename\n    }\n    total\n    mayHaveAdultContent\n    categoryFullMatch\n    __typename\n  }\n}\n\nfragment FacetFragment on Facet {\n  filter {\n    id\n    title\n    type\n    measurementUnit\n    description\n    __typename\n  }\n  buckets {\n    filterValue {\n      id\n      description\n      image\n      name\n      __typename\n    }\n    total\n    __typename\n  }\n  range {\n    min\n    max\n    __typename\n  }\n  __typename\n}\n\nfragment CategoryFragment on Category {\n  id\n  icon\n  parent {\n    id\n    __typename\n  }\n  seo {\n    header\n    metaTag\n    __typename\n  }\n  title\n  adult\n  __typename\n}\n\nfragment CategoryShortFragment on Category {\n  id\n  parent {\n    id\n    title\n    __typename\n  }\n  title\n  __typename\n}\n\nfragment SkuGroupCardFragment on SkuGroupCard {\n  ...DefaultCardFragment\n  photos {\n    key\n    link(trans: PRODUCT_540) {\n      high\n      low\n      __typename\n    }\n    previewLink: link(trans: PRODUCT_240) {\n      high\n      low\n      __typename\n    }\n    __typename\n  }\n  badges {\n    ... on BottomTextBadge {\n      backgroundColor\n      description\n      id\n      link\n      text\n      textColor\n      __typename\n    }\n    __typename\n  }\n  characteristicValues {\n    id\n    value\n    title\n    characteristic {\n      values {\n        id\n        title\n        value\n        __typename\n      }\n      title\n      id\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nfragment DefaultCardFragment on CatalogCard {\n  adult\n  favorite\n  feedbackQuantity\n  id\n  minFullPrice\n  minSellPrice\n  offer {\n    due\n    icon\n    text\n    textColor\n    __typename\n  }\n  badges {\n    backgroundColor\n    text\n    textColor\n    __typename\n  }\n  ordersQuantity\n  productId\n  rating\n  title\n  __typename\n}";
        UzumSearchQuery.Variables variables = UzumSearchQuery.Variables.builder()
                .queryInput(UzumSearchQuery.QueryInput.builder()
                        .categoryId(categoryId)
                        .filters(Collections.emptyList())
                        .showAdultContent("TRUE")
                        .sort("BY_RELEVANCE_DESC")
                        .pagination(UzumSearchQuery.Pagination.builder()
                                .limit(limit).offset(offset).build()).build()
                )
                .build();
        UzumSearchQuery searchQuery = UzumSearchQuery.builder()
                .variables(variables)
                .query(query)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(searchQuery);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String base64Body = Base64.getEncoder().encodeToString(bytes);
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "Accept-Language", "ru-RU",
                        "x-iid", "random_uuid()",
                        "Content-Type", "application/json",
                        "apollographql-client-name", "web-customers",
                        "apollographql-client-version", "1.5.9")).build();
        ProxyRequestParams.ContextValue content = ProxyRequestParams.ContextValue.builder()
                .key("content")
                .value(base64Body)
                .build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .url("https://graphql.umarket.uz/")
                .httpMethod(HttpMethod.POST.name())
                .context(List.of(headers, content))
                .build();
        try {
            Thread.sleep(randomTimeout.nextLong(fromTimeout, timeout));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return proxyService
                .getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<UzumGQLResponse>>() {
                }).getBody();
    }

    public Set<Long> getIdsByGql() {
        Set<Long> ids = getIds();
        Set<Long> categoryIds = new CopyOnWriteArraySet<>();
        List<Callable<Void>> callables = new ArrayList<>();
        for (Long id : ids) {
            callables.add(processGqlForIds(id, categoryIds));
        }
        List<Future<Void>> futures = callables.stream()
                .map(taskExecutor::submit)
                .toList();
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Multithreading error", e);
            }
        }
        log.info("Collected {} ids from GQL requests", categoryIds.size());
        return categoryIds;
    }

    public UzumGQLResponse retryableGQLRequest(long categoryId, long offset, long limit) {
        return retryTemplate.execute((RetryCallback<UzumGQLResponse, UzumGqlRequestException>) retryContext -> {
            UzumGQLResponse response = getGQLSearchResponse(String.valueOf(categoryId), offset, limit);
            if (!CollectionUtils.isEmpty(response.getErrors())) {
                for (UzumGQLResponse.GQLError error : response.getErrors()) {
                    if (error.getMessage().contains("offset")) {
                        log.warn("Finished collecting data for id - {}, " +
                                "because of response error object with message - {}", categoryId, error.getMessage());
                        return null;
                    } else if (error.getMessage().contains("429")) {
                        log.warn("Got 429 http status from request for category id {}", categoryId);
                        throw new UzumGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                    }
                }
            }
            return response;
        });
    }

    private Callable<Void> processGqlForIds(Long id, Set<Long> ids) {
        return () -> {
            UzumGQLResponse gqlResponse = retryableGQLRequest(id, 0L, 0L);
            for (UzumGQLResponse.ResponseCategoryWrapper categoryWrapper : gqlResponse.getData().getMakeSearch().getCategoryTree()) {
                ids.add(categoryWrapper.getCategory().getId());
            }
            return null;
        };
    }

    private void extractIds(UzumCategory.Data data, Set<Long> ids) {
        ids.add(data.getId());
        for (UzumCategory.Data child : data.getChildren()) {
            ids.add(child.getId());
        }
    }


    private Callable<Void> extractIdsAsync(UzumCategory.Data data, Set<Long> ids, boolean all) {
        return () -> {
            if (all) {
                extractAllIds(data, ids);
            } else {
                extractIds(data, ids);
            }
            return null;
        };
    }

    private Callable<Void> extractIdsAsync(UzumCategory.Data data, Set<Long> ids) {
        return () -> {
            extractIds(data, ids);
            return null;
        };
    }

    private Callable<Void> extractAllIdsAsync(UzumCategory.Data data, Set<Long> ids) {
        return () -> {
            extractAllIds(data, ids);
            return null;
        };
    }

    private void extractAllIds(UzumCategory.Data data, Set<Long> ids) {
        ids.add(data.getId());
        for (UzumCategory.Data child : data.getChildren()) {
            extractAllIds(child, ids);
        }
    }
}
