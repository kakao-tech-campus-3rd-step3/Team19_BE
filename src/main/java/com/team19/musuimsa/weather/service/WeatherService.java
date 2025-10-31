package com.team19.musuimsa.weather.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.weather.dto.KmaResponse;
import com.team19.musuimsa.weather.dto.KmaResponse.Header;
import com.team19.musuimsa.weather.dto.KmaResponse.Item;
import com.team19.musuimsa.weather.dto.NxNy;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.util.KmaGrid;
import com.team19.musuimsa.weather.util.KmaTime;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestClient restClient;

    private static final String KMA_SUCCESS_CODE = "00";

    @Value("${weather.kma.auth-key}")
    private String authKey;

    @Value("${weather.kma.base-url}")
    private String baseUrl;

    @PostConstruct
    void logConfig() {
        log.info("[WeatherService] KMA baseUrl = {}", baseUrl);
    }

    // 최신 기준시각에서 조회 실패/무자료면 -1h, -2h 순으로 폴백. KMA가 갓 갱신된 시각에 데이터를 늦게 올리는 경우를 흡수한다.
    @Cacheable(cacheManager = "caffeineCacheManager", cacheNames = "weather",
            key = "#root.target.gridKey(#latitude, #longitude) + ':t1h'")
    public WeatherResponse getCurrentTemp(double latitude, double longitude) {
        NxNy grid = KmaGrid.fromLatLon(latitude, longitude);
        Clock kstClock = Clock.system(ZoneId.of("Asia/Seoul"));

        KmaTime.Base baseTime = KmaTime.latestBase(kstClock);

        // 0h(현재 기준), -1h, -2h 순서로 시도
        Double t1h = tryFetchWithFallbacks(baseTime, grid, 0, 1, 2);
        KmaTime.Base usedBase = baseTime;

        if (t1h == null) {
            // 어떤 시각에서도 못 받았으면 에러
            String requestInfo = "base=" + baseTime.date() + " " + baseTime.time()
                    + ", nx=" + grid.nx() + ", ny=" + grid.ny();
            log.warn("기상청 응답에 현재기온이 없음. {}", requestInfo);
            throw new ExternalApiException(requestInfo);
        }

        return new WeatherResponse(t1h, usedBase.date(), usedBase.time());
    }

    public String gridKey(double latitude, double longitude) {
        NxNy grid = KmaGrid.fromLatLon(latitude, longitude);
        return grid.nx() + "-" + grid.ny();
    }

    private Double tryFetchWithFallbacks(KmaTime.Base base, NxNy grid, int... minusHours) {
        for (int h : minusHours) {
            KmaTime.Base b = (h == 0) ? base : KmaTime.minusHours(base, h);
            Double v = safeFetchT1H(b.date(), b.time(), grid.nx(), grid.ny());
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    // 외부 예외를 던지지 않고 null로 흘려보내 폴백을 유도
    private Double safeFetchT1H(String baseDate, String baseTime, int nx, int ny) {
        try {
            return fetchT1H(baseDate, baseTime, nx, ny);
        } catch (ExternalApiException e) {
            log.warn("KMA 호출 실패(폴백 예정). baseDate={}, baseTime={}, nx={}, ny={}, msg={}",
                    baseDate, baseTime, nx, ny, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("KMA 호출 예외(폴백 예정). {} - {} / baseDate={}, baseTime={}, nx={}, ny={}",
                    e.getClass().getSimpleName(), e.getMessage(), baseDate, baseTime, nx, ny);
            return null;
        }
    }

    // KMA resultCode != "00" 이면 예외를 던지지 않고 null 반환 → 상위 폴백 유도
    private Double fetchT1H(String baseDate, String baseTime, int nx, int ny) {
        URI uri = buildUri(baseDate, baseTime, nx, ny);
        String requestInfo = "base=" + baseDate + " " + baseTime + ", nx=" + nx + ", ny=" + ny;

        log.debug("KMA request uri={}", uri);

        KmaResponse kmaResponse = restClient.get().uri(uri).retrieve().body(KmaResponse.class);

        if (kmaResponse == null || kmaResponse.response() == null) {
            return null;
        }

        Header header = kmaResponse.response().header();
        if (header != null) {
            String code = header.resultCode();
            String message = header.resultMsg();

            if (code != null && !KMA_SUCCESS_CODE.equals(code)) {
                // null 반환하여 상위 폴백을 유도
                log.warn("KMA 무자료/오류 resultCode={}, resultMsg={}, requestInfo={}",
                        code, message, requestInfo);
                return null;
            }
        }

        if (kmaResponse.response().body() == null
                || kmaResponse.response().body().items() == null) {
            return null;
        }
        List<Item> list = kmaResponse.response().body().items().item();
        if (list == null) {
            return null;
        }

        // T1H 값 추출
        for (Item item : list) {
            if ("T1H".equals(item.category())) {
                String value = item.obsrValue();
                return value == null ? null : Double.valueOf(value);
            }
        }
        return null;
    }

    private URI buildUri(String baseDate, String baseTime, int nx, int ny) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/getUltraSrtNcst")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .queryParam("authKey", authKey)
                .build(true)
                .toUri();
    }
}