package org.springframework.cloud.kubernetes.discovery.ext.watcher.schedule;

import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LivenessScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LivenessScheduler.class);
	private static final String LABEL_IS_EXTERNAL_NAME = "external";

	private KubernetesClient kubernetesClient;
	private RestTemplate restTemplate;

	public LivenessScheduler(KubernetesClient kubernetesClient, RestTemplate restTemplate) {
		this.kubernetesClient = kubernetesClient;
		this.restTemplate = restTemplate;
	}

	@Scheduled(fixedRate = 10000)
	public void watch() {
		EndpointsList endpointsList = kubernetesClient.endpoints()
				.inNamespace(kubernetesClient.getNamespace())
				.list();
		endpointsList.getItems().forEach(it -> LOGGER.info("Endpoint: {}", it));
		endpointsList.getItems().stream()
				.filter(endpoints -> endpoints.getMetadata().getLabels().containsKey(LABEL_IS_EXTERNAL_NAME))
				.forEach(it -> {
					EndpointSubset subset = it.getSubsets().get(0);
					subset.getAddresses().forEach(endpointAddress -> {
						LOGGER.info("Calling: http://{}:{}", endpointAddress.getIp(), subset.getPorts().get(0).getPort());
					});
				});
	}

}
