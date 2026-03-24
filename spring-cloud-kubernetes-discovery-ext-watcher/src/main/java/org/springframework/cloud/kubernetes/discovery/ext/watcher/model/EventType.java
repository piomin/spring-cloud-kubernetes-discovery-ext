package org.springframework.cloud.kubernetes.discovery.ext.watcher.model;

public enum EventType {
    DISCOVERED,
    HEALTH_CHECK_SUCCESS,
    HEALTH_CHECK_FAILED,
    DEREGISTRATION_STARTED,
    DEREGISTRATION_COMPLETED
}
