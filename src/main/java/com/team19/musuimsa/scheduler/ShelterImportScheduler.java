package com.team19.musuimsa.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShelterImportScheduler {

    private final JobLauncher jobLauncher;
    private final Job shelterImportJob;

    // 매일 새벽 2시에 실행
    @Scheduled(cron = "0 45 3 * * *")
    public void runShelterImportJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();
        jobLauncher.run(shelterImportJob, jobParameters);
    }
}