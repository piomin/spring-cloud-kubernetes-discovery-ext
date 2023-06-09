package org.springframework.cloud.kubernetes.discovery.ext;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = KubernetesServiceRegistry.class)
@Import(KubernetesServiceRegistryTestsConfig.class)
@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceRegistryTests {

    static KubernetesClient client;

    @Autowired
    KubernetesServiceRegistry registry;
    @Autowired
    KubernetesDiscoveryProperties properties;

    @BeforeAll
    static void setup() {
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, client.getConfiguration().getMasterUrl());
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "default");
    }

    @Test
    void shouldRegisterNew() {
        KubernetesRegistration r = new KubernetesRegistration(properties);
        r.setMetadata(Map.of("namespace", "default", "name", "test"));
        r.setServiceId("test");
        r.setHost("192.168.1.20");
        r.setPort(8080);
        r.setInstanceId("test-1");
        registry.register(r);

        List<Service> services = client.services().inNamespace("default")
                .list().getItems();
        assertTrue(services.size() > 0);
        assertEquals("test", services.get(0).getMetadata().getName());
    }
}
