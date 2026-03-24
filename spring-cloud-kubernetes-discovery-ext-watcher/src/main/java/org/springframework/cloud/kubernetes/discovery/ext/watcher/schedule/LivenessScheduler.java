package org.springframework.cloud.kubernetes.discovery.ext.watcher.schedule;

import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.KubernetesRegistration;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.service.DiscoveryEventStore;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.task.DeactivateServiceTask;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LivenessScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LivenessScheduler.class);
    private static final String LABEL_IS_EXTERNAL_NAME = "external";

    private DeactivateServiceTask task;
    private KubernetesClient kubernetesClient;
    private RestTemplate restTemplate;
    private List<String> watchedUrls;
    private DiscoveryEventStore eventStore;
    private Set<String> discoveredServices;

    public LivenessScheduler(KubernetesClient kubernetesClient, RestTemplate restTemplate,
                            DeactivateServiceTask task, List<String> watchedUrls, DiscoveryEventStore eventStore) {
        this.kubernetesClient = kubernetesClient;
        this.restTemplate = restTemplate;
        this.task = task;
        this.watchedUrls = watchedUrls;
        this.eventStore = eventStore;
        this.discoveredServices = new HashSet<>();
    }

    @Scheduled(fixedRate = 10000)
    public void watch() {
        EndpointsList endpointsList = kubernetesClient.endpoints()
                .inNamespace(kubernetesClient.getNamespace())
                .list();
        endpointsList.getItems().stream()
                .filter(endpoints -> endpoints.getMetadata().getLabels().containsKey(LABEL_IS_EXTERNAL_NAME))
                .forEach(it -> {
                    if (!it.getSubsets().isEmpty()) {
                        EndpointSubset subset = it.getSubsets().get(0);
                        subset.getAddresses().forEach(endpointAddress -> {
                            String url = "http://" + endpointAddress.getIp() + ":" + subset.getPorts().get(0)
                                    .getPort() + "/actuator/health";
                            String serviceKey = it.getMetadata().getName() + ":" + endpointAddress.getIp() + ":" + subset.getPorts().get(0).getPort();

                            // Record DISCOVERED event for first-time detection
                            if (!discoveredServices.contains(serviceKey)) {
                                discoveredServices.add(serviceKey);
                                eventStore.addEvent(new DiscoveryEvent(
                                    EventType.DISCOVERED,
                                    it.getMetadata().getName(),
                                    endpointAddress.getIp(),
                                    subset.getPorts().get(0).getPort(),
                                    "External endpoint discovered"
                                ));
                                LOGGER.info("Discovered new external endpoint: {}", url);
                            }

                            if (!watchedUrls.contains(url)) {
                                ResponseEntity<String> responseEntity = null;
                                try {
                                    responseEntity = restTemplate.getForEntity(url, String.class);
                                    LOGGER.info("Active endpoint check: url->{}, status->{}", url, responseEntity.getStatusCode().value());
                                } catch (Exception e) {
                                    LOGGER.info("Error connecting to endpoint: {}", url);
                                }

                                if (responseEntity != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                                    // Record HEALTH_CHECK_SUCCESS
                                    eventStore.addEvent(new DiscoveryEvent(
                                        EventType.HEALTH_CHECK_SUCCESS,
                                        it.getMetadata().getName(),
                                        endpointAddress.getIp(),
                                        subset.getPorts().get(0).getPort(),
                                        "Health check passed"
                                    ));
                                } else {
                                    // Record HEALTH_CHECK_FAILED
                                    eventStore.addEvent(new DiscoveryEvent(
                                        EventType.HEALTH_CHECK_FAILED,
                                        it.getMetadata().getName(),
                                        endpointAddress.getIp(),
                                        subset.getPorts().get(0).getPort(),
                                        "Health check failed - triggering deregistration"
                                    ));
                                    task.process(url, create(endpointAddress.getIp(), subset.getPorts().get(0)
                                            .getPort(), it.getMetadata().getName()));
                                    watchedUrls.add(url);
                                }
                            }
                        });
                    }
                });
    }

    private KubernetesRegistration create(String ip, int port, String name) {
        KubernetesRegistration registration = new KubernetesRegistration();
        registration.setServiceId(name);
        registration.setHost(ip);
        registration.setPort(port);
        registration.getMetadata().put("name", name);
        return registration;
    }

}
