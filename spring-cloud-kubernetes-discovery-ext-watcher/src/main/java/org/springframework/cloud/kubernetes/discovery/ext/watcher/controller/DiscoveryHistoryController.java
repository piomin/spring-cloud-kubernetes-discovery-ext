package org.springframework.cloud.kubernetes.discovery.ext.watcher.controller;

import org.springframework.cloud.kubernetes.discovery.ext.watcher.dto.DiscoveryEventDto;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.service.DiscoveryEventStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryHistoryController {

    private final DiscoveryEventStore eventStore;

    public DiscoveryHistoryController(DiscoveryEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @GetMapping("/history")
    public ResponseEntity<List<DiscoveryEventDto>> getHistory(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Validate eventType if provided
        EventType parsedEventType = null;
        if (eventType != null) {
            try {
                parsedEventType = EventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid eventType: " + eventType +
                        ". Valid values: DISCOVERED, HEALTH_CHECK_SUCCESS, HEALTH_CHECK_FAILED, " +
                        "DEREGISTRATION_STARTED, DEREGISTRATION_COMPLETED");
            }
        }

        // Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be >= 1");
        }

        // Get filtered events
        List<DiscoveryEvent> events = eventStore.getEventsWithFilters(serviceId, parsedEventType, from, to);

        // Sort by timestamp descending (newest first)
        List<DiscoveryEvent> sortedEvents = events.stream()
                .sorted(Comparator.comparing(DiscoveryEvent::getTimestamp).reversed())
                .collect(Collectors.toList());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, sortedEvents.size());

        if (start >= sortedEvents.size()) {
            // Return empty list if page is out of range
            return ResponseEntity.ok(List.of());
        }

        List<DiscoveryEventDto> result = sortedEvents.subList(start, end).stream()
                .map(DiscoveryEventDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
