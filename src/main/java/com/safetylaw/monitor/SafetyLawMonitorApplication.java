package com.safetylaw.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("com.safetylaw.monitor.mapper")
public class SafetyLawMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SafetyLawMonitorApplication.class, args);
    }
}
