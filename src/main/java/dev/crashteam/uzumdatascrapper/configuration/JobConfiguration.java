package dev.crashteam.uzumdatascrapper.configuration;

import dev.crashteam.uzumdatascrapper.job.AuthTokenHandlerJob;
import dev.crashteam.uzumdatascrapper.job.CacheHandler;
import dev.crashteam.uzumdatascrapper.job.category.CategoryJob;
import dev.crashteam.uzumdatascrapper.job.position.PositionMasterJob;
import dev.crashteam.uzumdatascrapper.job.product.ProductMasterJob;
import dev.crashteam.uzumdatascrapper.model.Constant;
import dev.crashteam.uzumdatascrapper.model.job.JobModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final Scheduler scheduler;

    @Value("${app.job.cron.product-job}")
    private String productJobCron;

    @Value("${app.job.cron.position-job}")
    private String positionJobCron;

    @Value("${app.job.cron.category-job}")
    private String categoryJobCron;

    @Value("${app.job.cron.delete-product-cache}")
    private String deleteProductCache;

    @Value("${app.job.cron.token-job}")
    private String tokenCacheCron;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduleJob(new JobModel(Constant.PRODUCT_MASTER_JOB_NAME, ProductMasterJob.class, productJobCron,
                Constant.PRODUCT_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.POSITION_MASTER_JOB_NAME, PositionMasterJob.class, positionJobCron,
                Constant.POSITION_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.CATEGORY_MASTER_JOB_NAME, CategoryJob.class, categoryJobCron,
                Constant.CATEGORY_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.DELETE_PRODUCT_CACHE_JOB_NAME, CacheHandler.class, deleteProductCache,
                Constant.DELETE_PRODUCT_CACHE_TRIGGER_NAME, "cache"));
        if (scheduler.checkExists(new JobKey(Constant.TOKEN_CACHE_JOB_NAME))) {
            scheduler.deleteJob(new JobKey(Constant.TOKEN_CACHE_JOB_NAME));
        }
    }

    private void scheduleJob(JobModel jobModel) {
        try {
            JobDetail jobDetail = getJobDetail(jobModel.getJobName(), jobModel.getJobClass());
            scheduler.addJob(jobDetail, true, true);
            if (!scheduler.checkExists(TriggerKey.triggerKey(jobModel.getTriggerName(), jobModel.getTriggerGroup()))) {
                scheduler.scheduleJob(getJobTrigger(jobDetail, jobModel.getCron(), jobModel.getTriggerName(), jobModel.getTriggerGroup()));
                log.info("Scheduled - {} with cron - {}", jobModel.getJobName(), jobModel.getCron());
            }
        } catch (SchedulerException e) {
            log.warn("Scheduler exception occurred with message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start job with exception ", e);
        }
    }

    private JobDetail getJobDetail(String jobName, Class<? extends Job> jobClass) {
        JobKey jobKey = new JobKey(jobName);
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey).build();
    }

    private CronTrigger getJobTrigger(JobDetail jobDetail, String cron, String name, String group) {
        return TriggerBuilder
                .newTrigger()
                .withSchedule(CronScheduleBuilder
                        .cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                        .inTimeZone(TimeZone.getTimeZone("UTC")))
                .withIdentity(TriggerKey.triggerKey(name, group))
                .forJob(jobDetail).build();
    }
}
