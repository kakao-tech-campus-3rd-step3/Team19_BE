package com.team19.musuimsa.weather.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.weather.dto.NxNy;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.util.KmaGrid;
import com.team19.musuimsa.weather.util.KmaTime;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

    @Value("${weather.service-key}")
    private String serviceKey;

    @Value("${weather.kma.base-url}")
    private String baseUrl;

    @PostConstruct
    void logConfig() {
        log.info("[WeatherService] KMA baseUrl = {}", baseUrl);
    }

    @Cacheable(cacheNames = "t1h", key = "#root.target.gridKey(#latitude, #longitude)")
    public WeatherResponse getCurrentTemp(double latitude, double longitude) {
        NxNy g = KmaGrid.fromLatLon(latitude, longitude);
        Clock kst = Clock.system(ZoneId.of("Asia/Seoul"));

        KmaTime.Base base = KmaTime.latestBase(kst);
        Double t1h = fetchT1H(base.date(), base.time(), g.nx(), g.ny());

        if (t1h == null) {
            KmaTime.Base prev = KmaTime.minusHours(base, 1);
            Double fallback = fetchT1H(prev.date(), prev.time(), g.nx(), g.ny());
            if (fallback != null) {
                base = prev;
                t1h = fallback;
            }
        }

        if (t1h == null) {
            String requestInfo =
                    "base=" + base.date() + " " + base.time() + ", nx=" + g.nx() + ", ny=" + g.ny();
            String message = "기상청 응답에 현재기온이 없음. " + requestInfo;

            log.warn("{} / request info: {}", message, requestInfo);

            throw new ExternalApiException(requestInfo);
        }

        return new WeatherResponse(t1h, base.date(), base.time());
    }

    public String gridKey(double latitude, double longitude) {
        NxNy g = KmaGrid.fromLatLon(latitude, longitude);
        return g.nx() + "-" + g.ny();
    }

    @SuppressWarnings("unchecked")
    private Double fetchT1H(String baseDate, String baseTime, int nx, int ny) {
        URI uri = buildUri(baseDate, baseTime, nx, ny);
        String requestInfo = "base=" + baseDate + " " + baseTime + ", nx=" + nx + ", ny=" + ny;

        try {
            Map<String, Object> resp = restClient.get().uri(uri).retrieve().body(Map.class);
            if (resp == null) {
                return null;
            }

            Map<String, Object> response = (Map<String, Object>) resp.get("response");
            if (response == null) {
                return null;
            }

            Map<String, Object> header = (Map<String, Object>) response.get("header");
            if (header != null) {
                Object code = header.get("resultCode");
                Object message = header.get("resultMsg");
                if (code != null && !"00".equals(String.valueOf(code))) {
                    log.warn("기상청 오류 resultCode={}, resultMsg={}, requestInfo={}", code, message,
                            requestInfo);

                    throw new ExternalApiException(requestInfo);
                }
            }

            Map<String, Object> body = (Map<String, Object>) response.get("body");
            if (body == null) {
                return null;
            }
            Map<String, Object> items = (Map<String, Object>) body.get("items");
            if (items == null) {
                return null;
            }
            List<Map<String, Object>> list = (List<Map<String, Object>>) items.get("item");
            if (list == null) {
                return null;
            }

            for (Map<String, Object> m : list) {
                if ("T1H".equals(m.get("category"))) {
                    Object v = m.get("obsrValue");
                    return v == null ? null : Double.valueOf(String.valueOf(v));
                }
            }
            return null;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("기상청 호출 실패: {} - {} / requestInfo={}", e.getClass().getSimpleName(),
                    e.getMessage(),
                    requestInfo);

            throw new ExternalApiException(requestInfo);
        }
    }

    private URI buildUri(String baseDate, String baseTime, int nx, int ny) {
        return UriComponentsBuilder.fromHttpUrl(
                        baseUrl + "/getUltraSrtNcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 60)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();
    }
}