package dev.crashteam.uzumdatascrapper.job.product;

import dev.crashteam.uzumdatascrapper.model.Constant;
import dev.crashteam.uzumdatascrapper.service.ProductDataService;
import dev.crashteam.uzumdatascrapper.service.SimpleTriggerJobCreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMasterJob implements Job {

    private final SimpleTriggerJobCreatorService creatorService;
    private final ProductDataService productDataService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        productDataService.delete();
        creatorService.createLightJob(Constant.PRODUCT_JOB_NAME, Constant.CATEGORY_ID_KEY, Constant.PRODUCT_CATEGORY_MAP_KEY, ProductJob.class);
    }
}

