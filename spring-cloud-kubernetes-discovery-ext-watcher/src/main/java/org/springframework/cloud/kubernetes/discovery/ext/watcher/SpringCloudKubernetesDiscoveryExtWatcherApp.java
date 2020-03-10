package org.springframework.cloud.kubernetes.discovery.ext.watcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringCloudKubernetesDiscoveryExtWatcherApp {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudKubernetesDiscoveryExtWatcherApp.class, args);
	}

}
