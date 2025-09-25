package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ShelterImportBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job shelterImportJob(Step shelterUpdateStep, ShelterUpdateJobListener listener) {
        return new JobBuilder("shelterImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(shelterUpdateStep)
                .build();
    }

    @Bean
    public Step shelterUpdateStep(
            JpaPagingItemReader<Shelter> shelterItemReader,
            ItemProcessor<Shelter, Shelter> shelterItemProcessor,
            JpaItemWriter<Shelter> shelterItemWriter) {
        return new StepBuilder("shelterUpdateStep", jobRepository)
                .<Shelter, Shelter>chunk(CHUNK_SIZE, transactionManager)
                .reader(shelterItemReader)
                .processor(shelterItemProcessor)
                .writer(shelterItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Shelter> shelterItemReader() {
        return new JpaPagingItemReaderBuilder<Shelter>()
                .name("shelterItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT s FROM Shelter s ORDER BY s.shelterId")
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Shelter, Shelter> shelterItemProcessor(ShelterUpdateJobListener listener) {
        Map<Long, ExternalShelterItem> externalDataMap = listener.getExternalShelterData();
        return shelter -> {
            ExternalShelterItem externalData = externalDataMap.get(shelter.getShelterId());
            if (externalData == null) {
                // 외부 API에 없는 데이터는 건너뜀 (또는 삭제 로직 추가 가능)
                return null;
            }

            LocalTime weekdayOpen = parseTime(externalData.wkdayOperBeginTime());
            LocalTime weekdayClose = parseTime(externalData.wkdayOperEndTime());
            LocalTime weekendOpen = parseTime(externalData.wkendHdayOperBeginTime());
            LocalTime weekendClose = parseTime(externalData.wkendHdayOperEndTime());

            boolean isUpdated = shelter.updateShelterInfo(externalData, weekdayOpen, weekdayClose,
                    weekendOpen, weekendClose);

            return isUpdated ? shelter : null; // 변경된 경우에만 writer로 전달
        };
    }

    @Bean
    @StepScope
    public JpaItemWriter<Shelter> shelterItemWriter() {
        return new JpaItemWriterBuilder<Shelter>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    public static LocalTime parseTime(String raw) {
        if (raw == null) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 3) {
            digits = "0" + digits;
        }

        return LocalTime.parse(digits, DateTimeFormatter.ofPattern("HHmm"));
    }
}
