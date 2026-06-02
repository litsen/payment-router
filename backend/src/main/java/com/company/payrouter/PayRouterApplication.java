package com.company.payrouter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.company.payrouter.modules.**.mapper")
@EnableScheduling
@SpringBootApplication
public class PayRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayRouterApplication.class, args);
    }
}
