package dev.crashteam.uzumdatascrapper.model.job;

import lombok.Data;
import org.quartz.Job;

@Data
public class JobModel {

    private String jobName;
    private Class<? extends Job> jobClass;
    private String cron;
    private String triggerName;
    private String triggerGroup;

    public JobModel(String jobName, Class<? extends Job> jobClass, String cron, String triggerName, String triggerGroup) {
        this.jobName = jobName;
        this.jobClass = jobClass;
        this.cron = cron;
        this.triggerName = triggerName;
        this.triggerGroup = triggerGroup;
    }
}
