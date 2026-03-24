package org.springframework.cloud.kubernetes.discovery.ext.watcher.dto;

import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;

import java.time.format.DateTimeFormatter;

public class DiscoveryEventDto {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String id;
    private String timestamp;
    private EventType eventType;
    private String serviceId;
    private String host;
    private int port;
    private String message;

    public DiscoveryEventDto() {
    }

    public DiscoveryEventDto(String id, String timestamp, EventType eventType, String serviceId,
                             String host, int port, String message) {
        this.id = id;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.message = message;
    }

    public static DiscoveryEventDto from(DiscoveryEvent event) {
        return new DiscoveryEventDto(
                event.getId(),
                event.getTimestamp().format(ISO_FORMATTER),
                event.getEventType(),
                event.getServiceId(),
                event.getHost(),
                event.getPort(),
                event.getMessage()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
