package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.service.ShelterImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ShelterImportBatchConfig {

    private final ShelterImportService shelterImportService;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job shelterImportJob() {
        return new JobBuilder("shelterImportJob", jobRepository)
                .start(shelterImportStep())
                .build();
    }

    @Bean
    public Step shelterImportStep() {
        return new StepBuilder("shelterImportStep", jobRepository)
                .tasklet(shelterImportTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet shelterImportTasklet() {
        return (contribution, chunkContext) -> {
            shelterImportService.importOnce();
            return RepeatStatus.FINISHED;
        };
    }
}
