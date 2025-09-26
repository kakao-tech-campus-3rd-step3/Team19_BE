package com.team19.musuimsa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MusuimsaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusuimsaApplication.class, args);
    }

}
