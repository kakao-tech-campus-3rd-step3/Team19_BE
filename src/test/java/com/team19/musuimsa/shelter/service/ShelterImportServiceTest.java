package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelterImportServiceTest {

    @Mock
    ShelterOpenApiClient client;
    @Mock
    EntityManager entityManager;
    @Mock
    ShelterRepository shelterRepository;
    @Mock
    CacheManager cacheManager;
    @Mock
    Cache sheltersCache;

    @InjectMocks
    ShelterImportService service;

    @DisplayName("importOnce - 두 페이지를 돌아가며 저장된 전체 건수를 합산한다. ")
    @Test
    void importOnce_savesAcrossPages_andAccumulatesCount() {
        ExternalResponse page1 = resp(2, 1, 3, List.of(
                item(1001L, "A", "서울특별시", bd(37.1), bd(127.1),
                        10, 1, 2, "0900", "1800", "1000", "1700", "002"),
                item(1002L, "B", "부산광역시", bd(35.1), bd(129.1),
                        20, 0, 1, "0830", "2000", null, null, "001")
        ));
        ExternalResponse page2 = resp(2, 2, 3, List.of(
                item(1003L, "C", "대전광역시", bd(36.3), bd(127.4),
                        30, 2, 0, "900", "2130", "1100", "2300", "002")
        ));

        when(client.fetchPage(1)).thenReturn(page1);
        when(client.fetchPage(2)).thenReturn(page2);

        int saved = service.importOnce();
        assertThat(saved).isEqualTo(3);

        verify(entityManager, times(3)).persist(any(Shelter.class));
        verifyNoInteractions(shelterRepository);

        verify(client).fetchPage(1);
        verify(client).fetchPage(2);
        verifyNoMoreInteractions(client);
    }

    @DisplayName("importOnce - 응답 body 가 null이면 저장 없이 0을 반환한다. ")
    @Test
    void importOnce_returnsZero_whenBodyNull() {
        ExternalResponse page1 = new ExternalResponse(
                new ExternalResponse.Header("OK", "00", null),
                2, 1, 0, null // body = null
        );
        when(client.fetchPage(1)).thenReturn(page1);

        int saved = service.importOnce();
        assertThat(saved).isEqualTo(0);

        verify(entityManager, never()).persist(any());
        verifyNoInteractions(shelterRepository);
    }

    @DisplayName("importOnce - 외부 API 예외 발생 시 중단하고 누적 저장 수를 반환한다. ")
    @Test
    void importOnce_stopsOnExternalApiException() {
        ExternalResponse page1 = resp(2, 1, 4, List.of(
                item(2001L, "X", "주소1", bd(37), bd(127),
                        5, 0, 0, "0800", "1700", null, null, "001")
        ));

        // 첫 페이지는 정상, 두 번째에서 예외
        when(client.fetchPage(1)).thenReturn(page1);
        when(client.fetchPage(2)).thenThrow(
                new ExternalApiException("GET /DSSP-IF-10942?pageNo=2"));

        assertThatThrownBy(() -> service.importOnce())
                .isInstanceOf(ExternalApiException.class);
        verify(entityManager, times(1)).persist(any(Shelter.class));
        verifyNoInteractions(shelterRepository);

        verify(client).fetchPage(1);
        verify(client).fetchPage(2);
        verifyNoMoreInteractions(client);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    private static ExternalShelterItem item(
            Long id, String name, String addr, BigDecimal la, BigDecimal lo,
            Integer capacity, Integer fan, Integer ac,
            String wkOpen, String wkClose, String weOpen, String weClose,
            String type
    ) {
        return new ExternalShelterItem(
                id, name, addr, la, lo,
                capacity, fan, ac,
                wkOpen, wkClose, weOpen, weClose, type
        );
    }

    private static ExternalResponse resp(Integer numOfRows,
                                         Integer pageNo,
                                         Integer totalCount,
                                         List<ExternalShelterItem> body
    ) {
        return new ExternalResponse(
                new ExternalResponse.Header("OK", "00", null),
                numOfRows, pageNo, totalCount, body
        );
    }
}
