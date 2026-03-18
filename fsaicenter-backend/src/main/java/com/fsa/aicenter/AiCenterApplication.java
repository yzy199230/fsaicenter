package com.fsa.aicenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AiCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCenterApplication.class, args);
    }
}
