package com.renuka.notification_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotificationBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationBackendApplication.class, args);
	}

}
