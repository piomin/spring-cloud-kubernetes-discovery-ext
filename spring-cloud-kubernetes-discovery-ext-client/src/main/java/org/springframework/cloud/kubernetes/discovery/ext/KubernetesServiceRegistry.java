package org.springframework.cloud.kubernetes.discovery.ext;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;

import java.util.Collections;

public class KubernetesServiceRegistry implements ServiceRegistry<KubernetesRegistration> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceRegistry.class);
    private static final String LABEL_IS_EXTERNAL_NAME = "external";

    private final KubernetesClient client;
    private KubernetesDiscoveryProperties properties;

    public KubernetesServiceRegistry(KubernetesClient client, KubernetesDiscoveryProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void register(KubernetesRegistration registration) {
        LOG.info("Registering service with kubernetes: " + registration.getServiceId());
        Resource<Endpoints> resource = client.endpoints()
                .inNamespace(registration.getMetadata().get("namespace"))
                .withName(registration.getMetadata().get("name"));
        Endpoints endpoints = resource.get();
        if (endpoints == null) {
            Service s = new ServiceBuilder().withNewMetadata()
                    .withName(registration.getMetadata().get("name"))
                    .withLabels(Collections.singletonMap("app", registration.getMetadata().get("name")))
                    .withAnnotations(Collections.singletonMap("healthUrl", "/actuator/health"))
                    .endMetadata()
                    .withNewSpec()
                        .addNewPort().withPort(registration.getPort()).withProtocol("TCP").endPort()
                        .addToSelector(Collections.singletonMap("app", registration.getMetadata().get("name")))
                    .endSpec().build();

            Service ss = client.services().inNamespace(registration.getMetadata().get("namespace"))
                    .create(s);
            LOG.info("New service: {}", ss);
            Endpoints e = client.endpoints().createOrReplace(create(registration));
            LOG.info("New endpoint: {}", e);
        } else {
            try {
                for (EndpointSubset s : endpoints.getSubsets()) {
                   if (s.getPorts().contains(registration.getPort())) {
                       s.getAddresses().add(new EndpointAddressBuilder().withIp(registration.getHost()).build());
                   }
                }
                resource.edit().setSubsets(endpoints.getSubsets());

                LOG.info("Endpoint updated: {}", resource.get());
            } catch (RuntimeException e) {
                EndpointSubset es = new EndpointSubsetBuilder().addNewPort().withPort(registration.getPort()).endPort()
                        .addNewAddress().withIp(registration.getHost()).endAddress()
                        .build();
                resource.edit().getSubsets().add(es);
//                Endpoints updatedEndpoints = resource.edit().getSubsets();
//                        .addNewSubset()
//                        .withPorts(new EndpointPortBuilder().withPort(registration.getPort()).build())
//                        .withAddresses(new EndpointAddressBuilder().withIp(registration.getHost()).build())
//                        .endSubset()
//                        .done();
                LOG.info("Endpoint updated: {}", resource.get());
            }
        }

    }

    @Override
    public void deregister(KubernetesRegistration registration) {
        LOG.info("De-registering service with kubernetes: " + registration.getInstanceId());
        Resource<Endpoints> resource = client.endpoints()
                .inNamespace(registration.getMetadata().get("namespace"))
                .withName(registration.getMetadata().get("name"));

        EndpointAddress address = new EndpointAddressBuilder().withIp(registration.getHost()).build();
        for (EndpointSubset s : resource.get().getSubsets()) {
            if (s.getPorts().contains(registration.getPort())) {
                s.getAddresses().remove(address);
            }
        }
        resource.edit().setSubsets(resource.get().getSubsets());
//                .editMatchingSubset(builder -> builder.hasMatchingPort(v -> v.getPort().equals(registration.getPort())))
//                .removeFromAddresses(address)
//                .endSubset()
//                .done();
        LOG.info("Endpoint updated: {}", resource.get().getSubsets());

        // TODO - remove empty subset
//        resource.get().getSubsets().stream()
//                .filter(subset -> subset.getAddresses().size() == 0)
//                .forEach(subset -> resource.edit()
//                        .removeFromSubsets(subset)
//                        .done());
    }

    @Override
    public void close() {

    }

    @Override
    public void setStatus(KubernetesRegistration registration, String status) {

    }

    @Override
    public <T> T getStatus(KubernetesRegistration registration) {
        return null;
    }

    private Endpoints create(KubernetesRegistration registration) {
        EndpointAddress address = new EndpointAddressBuilder().withIp(registration.getHost()).build();
        EndpointPort port = new EndpointPortBuilder().withPort(registration.getPort()).build();
        EndpointSubset subset = new EndpointSubsetBuilder().withAddresses(address).withPorts(port).build();
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(registration.getMetadata().get("name"))
                .withNamespace(registration.getMetadata().get("namespace"))
                .withLabels(Collections.singletonMap(LABEL_IS_EXTERNAL_NAME, "true"))
                .build();
        Endpoints endpoints = new EndpointsBuilder().withSubsets(subset).withMetadata(metadata).build();
        return endpoints;
    }

}
