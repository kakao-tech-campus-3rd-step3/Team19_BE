package com.team19.musuimsa.shelter.scheduler;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelterDataSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job shelterDataSyncJob;

    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00에 실행
    public void runJob() {
        try {
            jobLauncher.run(
                    shelterDataSyncJob,
                    new JobParametersBuilder().addString("dateTime", LocalDateTime.now().toString())
                            .toJobParameters()
            );
        } catch (Exception e) {
            log.error("shelterDataSyncJob 실패: ", e);
        }
    }
}
