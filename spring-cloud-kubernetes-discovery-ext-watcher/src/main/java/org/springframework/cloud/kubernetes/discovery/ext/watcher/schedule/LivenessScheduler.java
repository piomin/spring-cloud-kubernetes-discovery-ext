package org.springframework.cloud.kubernetes.discovery.ext.watcher.schedule;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LivenessScheduler {

	private KubernetesClient kubernetesClient;

	public LivenessScheduler(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	@Scheduled(fixedRate = 10000)
	public void watch() {

	}

}
