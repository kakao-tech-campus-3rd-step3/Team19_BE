package com.team19.musuimsa.shelter.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ShelterDataSyncJobConfig {

    private final ShelterRepository shelterRepository;
    private final ShelterOpenApiClient shelterOpenApiClient;

    @Bean
    public Job shelterDataSyncJob(JobRepository jobRepository, Step shelterDataSyncStep) {
        return new JobBuilder("shelterDataSyncJob", jobRepository)
                .start(shelterDataSyncStep)
                .build();
    }

    @Bean
    public Step shelterDataSyncStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("shelterDataSyncStep", jobRepository)
                .<ExternalShelterItem, Shelter>chunk(100, transactionManager)
                .reader(shelterApiItemReader())
                .processor(shelterItemProcessor())
                .writer(shelterItemWriter())
                .build();
    }

    @Bean
    public ShelterApiItemReader shelterApiItemReader() {
        return new ShelterApiItemReader(shelterOpenApiClient);
    }

    @Bean
    public ItemProcessor<ExternalShelterItem, Shelter> shelterItemProcessor() {
        return Shelter::toShelter;
    }

    @Bean
    public ItemWriter<Shelter> shelterItemWriter() {
        return shelterRepository::saveAll;
    }
}
