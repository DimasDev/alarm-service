package com.example.alarmservice;

import com.example.alarmservice.notification.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NotificationProperties.class)
public class AlarmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlarmServiceApplication.class, args);
    }

}
