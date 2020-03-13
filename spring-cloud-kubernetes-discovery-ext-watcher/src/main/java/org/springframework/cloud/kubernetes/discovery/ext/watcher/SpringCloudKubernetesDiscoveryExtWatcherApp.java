package org.springframework.cloud.kubernetes.discovery.ext.watcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SpringCloudKubernetesDiscoveryExtWatcherApp {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudKubernetesDiscoveryExtWatcherApp.class, args);
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplateBuilder().build();
	}

	@Bean
	List<String> watchedUrls() {
		return new ArrayList<>();
	}

}
