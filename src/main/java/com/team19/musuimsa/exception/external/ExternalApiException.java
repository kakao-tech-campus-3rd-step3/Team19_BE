package com.team19.musuimsa.exception.external;

public class ExternalApiException extends RuntimeException {
    private final String url;

    public ExternalApiException(String url) {
        super("외부 API 호출 실패: " + url);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
