package org.springframework.cloud.kubernetes.discovery.ext.watcher.service;

import org.springframework.cloud.kubernetes.discovery.ext.watcher.config.KubernetesWatcherProperties;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiscoveryEventStore {

    private final List<DiscoveryEvent> events;
    private final KubernetesWatcherProperties properties;

    public DiscoveryEventStore(KubernetesWatcherProperties properties) {
        this.properties = properties;
        this.events = Collections.synchronizedList(new LinkedList<>());
    }

    public synchronized void addEvent(DiscoveryEvent event) {
        if (!properties.isHistoryEnabled()) {
            return;
        }

        events.add(event);

        // Remove oldest event if max size exceeded (circular buffer)
        if (events.size() > properties.getHistoryMaxSize()) {
            events.remove(0);
        }
    }

    public synchronized List<DiscoveryEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    public synchronized List<DiscoveryEvent> getEventsByServiceId(String serviceId) {
        if (serviceId == null) {
            return getAllEvents();
        }
        return events.stream()
                .filter(event -> serviceId.equals(event.getServiceId()))
                .collect(Collectors.toList());
    }

    public synchronized List<DiscoveryEvent> getEventsByType(EventType eventType) {
        if (eventType == null) {
            return getAllEvents();
        }
        return events.stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .collect(Collectors.toList());
    }

    public synchronized List<DiscoveryEvent> getEventsByTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return getAllEvents();
        }
        return events.stream()
                .filter(event -> {
                    LocalDateTime timestamp = event.getTimestamp();
                    boolean afterFrom = from == null || !timestamp.isBefore(from);
                    boolean beforeTo = to == null || !timestamp.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .collect(Collectors.toList());
    }

    public synchronized List<DiscoveryEvent> getEventsWithFilters(String serviceId, EventType eventType,
                                                                   LocalDateTime from, LocalDateTime to) {
        return events.stream()
                .filter(event -> {
                    boolean matchesService = serviceId == null || serviceId.equals(event.getServiceId());
                    boolean matchesType = eventType == null || eventType.equals(event.getEventType());

                    LocalDateTime timestamp = event.getTimestamp();
                    boolean afterFrom = from == null || !timestamp.isBefore(from);
                    boolean beforeTo = to == null || !timestamp.isAfter(to);

                    return matchesService && matchesType && afterFrom && beforeTo;
                })
                .collect(Collectors.toList());
    }
}
