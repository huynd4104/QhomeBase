package com.QhomeBase.servicescardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicesCardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicesCardServiceApplication.class, args);
    }

}
