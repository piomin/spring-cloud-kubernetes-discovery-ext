package org.springframework.cloud.kubernetes.discovery.ext;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class KubernetesServiceRegistryTestsConfig {

    @Bean
    KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

    @Bean
    @Primary
    KubernetesDiscoveryProperties kubernetesDiscoveryProperties() {
        return KubernetesDiscoveryProperties.DEFAULT;
    }

}
