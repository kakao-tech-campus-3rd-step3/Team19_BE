package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelterUpdateJobListenerTest {

    @Mock
    ShelterOpenApiClient api;
    @Mock
    ShelterPhotoService photo;
    @Mock
    ShelterRepository repo;
    @Mock
    CacheManager cacheManager;

    @Test
    @DisplayName("afterJob: Redis가 있을 때 변경된 셸터의 지오해시 패턴을 SCAN/DEL 한다. ")
    void afterJob_selectiveInvalidation_scansAndDeletesOnRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisConnectionFactory cf = mock(RedisConnectionFactory.class);
        RedisConnection conn = mock(RedisConnection.class);
        when(redis.getConnectionFactory()).thenReturn(cf);
        when(cf.getConnection()).thenReturn(conn);

        // 1) 커서 mock: 키 하나 반환 후 종료
        @SuppressWarnings("unchecked")
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("musuimsa::k1".getBytes());

        doNothing().when(cursor).close();

        // 2) scan() 이 커서를 돌려주도록 스텁
        when(conn.scan(any(ScanOptions.class))).thenReturn(cursor);

        // SUT
        ShelterUpdateJobListener listener =
                new ShelterUpdateJobListener(api, photo, repo, cacheManager, Optional.of(redis));

        JobExecution jobExecution = new JobExecution(1L);
        jobExecution.getExecutionContext().put(ShelterImportBatchConfig.LOCATION_UPDATED_IDS_KEY, Set.of(1L));

        when(repo.findAllById(Set.of(1L))).thenReturn(List.of(
                Shelter.builder()
                        .shelterId(1L)
                        .latitude(new BigDecimal("37.1"))
                        .longitude(new BigDecimal("127.2"))
                        .build()
        ));

        // when
        listener.afterJob(jobExecution);

        // then
        verify(redis, atLeastOnce()).delete(anySet());
    }

    @Test
    @DisplayName("afterJob: Dev(Caffeine) 환경에서는 redis가 없으므로 sheltersMap 전체를 clear 한다. ")
    void afterJob_devFallback_clearsCaffeine() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("sheltersMap")).thenReturn(cache);

        ShelterUpdateJobListener listener =
                new ShelterUpdateJobListener(api, photo, repo, cacheManager, Optional.empty());

        JobExecution jobExecution = new JobExecution(2L);
        jobExecution.getExecutionContext().put(ShelterImportBatchConfig.LOCATION_UPDATED_IDS_KEY, Set.of(1L));

        when(repo.findAllById(Set.of(1L))).thenReturn(List.of(
                Shelter.builder()
                        .shelterId(1L)
                        .latitude(new BigDecimal("37.1"))
                        .longitude(new BigDecimal("127.2"))
                        .build()
        ));

        listener.afterJob(jobExecution);

        verify(cache).clear();
    }
}
