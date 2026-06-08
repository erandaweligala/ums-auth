package com.adl.et.telco.crm.usermanagerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(com.adl.et.telco.dte.prom_spring_metrics_plugin.domain.metrics.autoconfigure.PromMetricsAutoConfiguration.class)
@SpringBootApplication
public class UsermanagerserviceApplication {
    public static void main(String[] args) {

        SpringApplication.run(UsermanagerserviceApplication.class, args);
    }
}


