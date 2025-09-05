package com.team19.musuimsa.exception.external;

public class ExternalApiException extends RuntimeException {
    private final String url;

    public ExternalApiException(String url, Throwable cause) {
        super("외부 API 호출 실패: " + url, cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
