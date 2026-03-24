package org.springframework.cloud.kubernetes.discovery.ext.watcher.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class DiscoveryEvent {

    private final String id;
    private final LocalDateTime timestamp;
    private final EventType eventType;
    private final String serviceId;
    private final String host;
    private final int port;
    private final String message;

    public DiscoveryEvent(EventType eventType, String serviceId, String host, int port, String message) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryEvent that = (DiscoveryEvent) o;
        return port == that.port &&
                Objects.equals(id, that.id) &&
                Objects.equals(timestamp, that.timestamp) &&
                eventType == that.eventType &&
                Objects.equals(serviceId, that.serviceId) &&
                Objects.equals(host, that.host) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, eventType, serviceId, host, port, message);
    }

    @Override
    public String toString() {
        return "DiscoveryEvent{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", eventType=" + eventType +
                ", serviceId='" + serviceId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", message='" + message + '\'' +
                '}';
    }
}
