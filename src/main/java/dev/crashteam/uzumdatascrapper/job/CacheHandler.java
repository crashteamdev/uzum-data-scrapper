package dev.crashteam.uzumdatascrapper.job;

import dev.crashteam.uzumdatascrapper.service.ProductDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class CacheHandler implements Job {

    private final ProductDataService productDataService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    }
}
