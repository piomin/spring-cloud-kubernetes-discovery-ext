package org.springframework.cloud.kubernetes.discovery.ext.watcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

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
