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

    @Cacheable(cacheNames = "t1h", key = "#root.target.gridKey(#latitude, #longitude)")
    public WeatherResponse getCurrentTemp(double latitude, double longitude) {
        NxNy grid = KmaGrid.fromLatLon(latitude, longitude);
        Clock kstClock = Clock.system(ZoneId.of("Asia/Seoul"));

        KmaTime.Base baseTime = KmaTime.latestBase(kstClock);
        Double t1h = fetchT1H(baseTime.date(), baseTime.time(), grid.nx(), grid.ny());

        if (t1h == null) {
            KmaTime.Base previousBaseTime = KmaTime.minusHours(baseTime, 1);
            Double fallback = fetchT1H(previousBaseTime.date(), previousBaseTime.time(), grid.nx(),
                    grid.ny());
            if (fallback != null) {
                baseTime = previousBaseTime;
                t1h = fallback;
            }
        }

        if (t1h == null) {
            String requestInfo =
                    "base=" + baseTime.date() + " " + baseTime.time() + ", nx=" + grid.nx()
                            + ", ny="
                            + grid.ny();
            String message = "기상청 응답에 현재기온이 없음. " + requestInfo;

            log.warn("{} / request info: {}", message, requestInfo);

            throw new ExternalApiException(requestInfo);
        }

        return new WeatherResponse(t1h, baseTime.date(), baseTime.time());
    }

    public String gridKey(double latitude, double longitude) {
        NxNy grid = KmaGrid.fromLatLon(latitude, longitude);
        return grid.nx() + "-" + grid.ny();
    }

    private Double fetchT1H(String baseDate, String baseTime, int nx, int ny) {
        URI uri = buildUri(baseDate, baseTime, nx, ny);
        String requestInfo = "base=" + baseDate + " " + baseTime + ", nx=" + nx + ", ny=" + ny;

        try {
            KmaResponse kmaResponse = restClient.get().uri(uri).retrieve().body(KmaResponse.class);

            if (kmaResponse == null || kmaResponse.response() == null) {
                return null;
            }

            Header header = kmaResponse.response().header();
            if (header != null) {
                String code = header.resultCode();
                String message = header.resultMsg();

                if (code != null && !KMA_SUCCESS_CODE.equals(code)) {
                    log.warn("기상청 오류 resultCode={}, resultMsg={}, requestInfo={}", code, message,
                            requestInfo);
                    throw new ExternalApiException(requestInfo);
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
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("기상청 호출 실패: {} - {} / requestInfo={}", e.getClass().getSimpleName(),
                    e.getMessage(), requestInfo);
            throw new ExternalApiException(requestInfo);
        }
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