package dev.crashteam.uzumdatascrapper.job;

import dev.crashteam.uzumdatascrapper.model.Constant;
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
public class ProductPositionMasterJob implements Job {

    private final SimpleTriggerJobCreatorService creatorService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        creatorService.createJob(Constant.PRODUCT_POSITION_JOB_NAME, Constant.PRODUCT_POSITION_KEY, PositionProductJob.class, true);
    }
}
