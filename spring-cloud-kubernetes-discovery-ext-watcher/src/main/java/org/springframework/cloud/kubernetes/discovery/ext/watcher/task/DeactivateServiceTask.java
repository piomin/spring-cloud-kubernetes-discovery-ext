package org.springframework.cloud.kubernetes.discovery.ext.watcher.task;

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
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

@Configuration
@Async
public class DeactivateServiceTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeactivateServiceTask.class);
    private static final int RETRIES = 3;
    private static final int RETRY_PERIOD = 3000;

    private KubernetesClient kubernetesClient;
    private RestTemplate restTemplate;

    public DeactivateServiceTask(KubernetesClient kubernetesClient, RestTemplate restTemplate) {
        this.kubernetesClient = kubernetesClient;
        this.restTemplate = restTemplate;
    }

    public void process(String url, KubernetesRegistration registration) {
        int retryCount = 0;
        boolean ok = true;
        while (restTemplate.getForEntity(url, Health.class).getStatusCodeValue() != 200) {
            if (++retryCount > RETRIES) {
                ok = false;
                break;
            }
            try {
                Thread.sleep(RETRY_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!ok)
            deregister(registration);
    }

    private void deregister(KubernetesRegistration registration) {
        LOGGER.info("De-registering service with kubernetes: " + registration.getInstanceId());
        Resource<Endpoints, DoneableEndpoints> resource = kubernetesClient.endpoints()
                .inNamespace(kubernetesClient.getNamespace())
                .withName(registration.getMetadata().get("name"));

        EndpointAddress address = new EndpointAddressBuilder().withIp(registration.getHost()).build();
        Endpoints updatedEndpoints = resource.edit()
                .editMatchingSubset(builder -> builder.hasMatchingPort(v -> v.getPort().equals(registration.getPort())))
                .removeFromAddresses(address)
                .endSubset()
                .done();
        LOGGER.info("Endpoint updated: {}", updatedEndpoints);

        resource.get().getSubsets().stream()
                .filter(subset -> subset.getAddresses().size() == 0)
                .forEach(subset -> resource.edit()
                        .removeFromSubsets(subset)
                        .done());
    }

}
