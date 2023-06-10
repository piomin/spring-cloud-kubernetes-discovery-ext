package org.springframework.cloud.kubernetes.discovery.ext.watcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.kubernetes.watcher")
public class KubernetesWatcherProperties {

    private String targetNamespace;
    private Boolean allNamespaces = false;
    private int retries = 3;
    private int retryTimeout = 1000;

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    public Boolean getAllNamespaces() {
        return allNamespaces;
    }

    public void setAllNamespaces(Boolean allNamespaces) {
        this.allNamespaces = allNamespaces;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(int retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
}
