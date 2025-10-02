package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ShelterImportBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 100;
    public static final String UPDATED_IDS_KEY = "updatedShelterIds";

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
            JpaItemWriter<Shelter> shelterItemWriter,
            ItemWriteListener<Shelter> updatedIdCollector) {
        return new StepBuilder("shelterUpdateStep", jobRepository)
                .<Shelter, Shelter>chunk(CHUNK_SIZE, transactionManager)
                .reader(shelterItemReader)
                .processor(shelterItemProcessor)
                .writer(shelterItemWriter)
                .listener(updatedIdCollector)
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
    public ItemProcessor<Shelter, Shelter> shelterItemProcessor(
            @Value("#{jobExecutionContext['externalShelterData']}") Map<Long, ExternalShelterItem> externalDataMap) {
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

    @Bean
    public ItemWriteListener<Shelter> updatedIdCollector() {
        return new ItemWriteListener<>() {

            @Override
            public void afterWrite(Chunk<? extends Shelter> items) {
                if (items.isEmpty()) {
                    return;
                }

                StepContext stepCtx = StepSynchronizationManager.getContext();

                ExecutionContext jobCtx = stepCtx.getStepExecution()
                        .getJobExecution()
                        .getExecutionContext();

                Set<Long> ids = (Set<Long>) jobCtx.get(UPDATED_IDS_KEY);
                if (ids == null) {
                    ids = new HashSet<>();
                    jobCtx.put(UPDATED_IDS_KEY, ids);
                }

                for (Shelter s : items.getItems()) {
                    ids.add(s.getShelterId());
                }
            }
        };
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
