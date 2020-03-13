package org.springframework.cloud.kubernetes.discovery.ext.watcher.task;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.DoneableEndpoints;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.KubernetesRegistration;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Async
public class DeactivateServiceTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeactivateServiceTask.class);
    private static final int RETRIES = 3;
    private static final int RETRY_PERIOD = 3000;

    private KubernetesClient kubernetesClient;
    private RestTemplate restTemplate;
    private List<String> watchedUrls;

    public DeactivateServiceTask(KubernetesClient kubernetesClient, RestTemplate restTemplate, List<String> watchedUrls) {
        this.kubernetesClient = kubernetesClient;
        this.restTemplate = restTemplate;
        this.watchedUrls = watchedUrls;
    }

    public void process(String url, KubernetesRegistration registration) {
        boolean ok = false;
        ResponseEntity<String> entity = null;
        for (int i = 0; i < RETRIES; i++) {
            try {
                entity = restTemplate.getForEntity(url, String.class);
            } catch (Exception e) {

            }
            if (entity == null || entity.getStatusCodeValue() != 200) {
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                ok = true;
                break;
            }
        }
        if (!ok) {
			deregister(registration);
			watchedUrls.remove(url);
		}
    }

    private void deregister(KubernetesRegistration registration) {
        LOGGER.info("De-registering service with kubernetes: " + registration.getMetadata().get("name"));
        Resource<Endpoints, DoneableEndpoints> resource = kubernetesClient.endpoints()
                .inNamespace(kubernetesClient.getNamespace())
                .withName(registration.getMetadata().get("name"));

        EndpointAddress address = new EndpointAddressBuilder().withIp(registration.getHost()).build();
		List<EndpointAddress> addressList = resource.get().getSubsets().get(0).getAddresses();
        addressList.remove(address);
        if (addressList.size() > 0) {
			Endpoints updatedEndpoints = resource.edit()
					.editMatchingSubset(builder -> builder
							.hasMatchingPort(v -> v.getPort().equals(registration.getPort())))
					.withAddresses(addressList)
					.endSubset()
					.done();
			LOGGER.info("Endpoint updated: {}", updatedEndpoints);
		} else {
			Endpoints updatedEndpoints = resource.edit()
					.withSubsets(new ArrayList<>())
					.done();
			LOGGER.info("Endpoint updated: {}", updatedEndpoints);
		}

    }

}
