package com.QhomeBase.customerinteractionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CustomerInteractionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerInteractionServiceApplication.class, args);
    }

}
