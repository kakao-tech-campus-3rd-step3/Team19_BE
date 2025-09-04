package com.team19.musuimsa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.transport.ProxyProvider;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientProxyConfig {

    @Bean("shelterWebClient")
    public WebClient shelterWebClient(
            @Value("${proxy.enabled:false}") boolean proxyEnabled,
            @Value("${proxy.socks.host:127.0.0.1}") String host,
            @Value("${proxy.socks.port:1080}") int port
    ) {
        HttpClient http = HttpClient.create();
        if (proxyEnabled) {
            http = http.proxy(p -> p.type(ProxyProvider.Proxy.SOCKS5)
                    .host(host).port(port));
        }
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}


