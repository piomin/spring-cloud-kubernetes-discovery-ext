package org.springframework.cloud.kubernetes.discovery.ext.watcher.schedule;

import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.KubernetesRegistration;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.task.DeactivateServiceTask;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class LivenessScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LivenessScheduler.class);
	private static final String LABEL_IS_EXTERNAL_NAME = "external";

	private DeactivateServiceTask task;
	private KubernetesClient kubernetesClient;
	private RestTemplate restTemplate;
	private List<String> watchedUrls = new ArrayList<>();

	public LivenessScheduler(KubernetesClient kubernetesClient, RestTemplate restTemplate, DeactivateServiceTask task) {
		this.kubernetesClient = kubernetesClient;
		this.restTemplate = restTemplate;
		this.task = task;
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
						String url = "http://" + endpointAddress.getIp() + ":" + subset.getPorts().get(0).getPort() + "/actuator/health";
						if (!watchedUrls.contains(url)) {
							ResponseEntity<Health> responseEntity = restTemplate.getForEntity(url, Health.class);
							LOGGER.info("Endpoint {}: status->{}", url, responseEntity.getStatusCodeValue());
							if (responseEntity.getStatusCode() != HttpStatus.OK) {
								task.process(url, create(endpointAddress.getIp(), subset.getPorts().get(0).getPort(), it.getMetadata().getName()));
								watchedUrls.add(url);
							}
						}
					});
				});
	}

	private KubernetesRegistration create(String ip, int port, String name) {
		KubernetesRegistration registration = new KubernetesRegistration();
		registration.setHost(ip);
		registration.setPort(port);
		registration.getMetadata().put("name", name);
		return registration;
	}

}
