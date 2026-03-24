package org.springframework.cloud.kubernetes.discovery.ext.watcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.config.KubernetesWatcherProperties;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryEventStoreTest {

    private DiscoveryEventStore eventStore;
    private KubernetesWatcherProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KubernetesWatcherProperties();
        properties.setHistoryEnabled(true);
        properties.setHistoryMaxSize(10);
        eventStore = new DiscoveryEventStore(properties);
    }

    @Test
    void testAddEvent() {
        DiscoveryEvent event = new DiscoveryEvent(EventType.DISCOVERED, "test-service", "192.168.1.1", 8080, "Service discovered");

        eventStore.addEvent(event);

        List<DiscoveryEvent> events = eventStore.getAllEvents();
        assertEquals(1, events.size());
        assertEquals(event, events.get(0));
    }

    @Test
    void testCircularBuffer() {
        // Add 15 events to a store with max size 10
        for (int i = 0; i < 15; i++) {
            DiscoveryEvent event = new DiscoveryEvent(EventType.DISCOVERED, "service-" + i, "192.168.1." + i, 8080, "Event " + i);
            eventStore.addEvent(event);
        }

        List<DiscoveryEvent> events = eventStore.getAllEvents();
        assertEquals(10, events.size());

        // Verify oldest events were removed (first 5 should be gone)
        assertEquals("service-5", events.get(0).getServiceId());
        assertEquals("service-14", events.get(9).getServiceId());
    }

    @Test
    void testFilterByServiceId() {
        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_SUCCESS, "service-b", "192.168.1.2", 8080, "Message 2"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-a", "192.168.1.1", 8080, "Message 3"));

        List<DiscoveryEvent> filtered = eventStore.getEventsByServiceId("service-a");

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(e -> "service-a".equals(e.getServiceId())));
    }

    @Test
    void testFilterByEventType() {
        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_SUCCESS, "service-b", "192.168.1.2", 8080, "Message 2"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-a", "192.168.1.1", 8080, "Message 3"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-c", "192.168.1.3", 8080, "Message 4"));

        List<DiscoveryEvent> filtered = eventStore.getEventsByType(EventType.HEALTH_CHECK_FAILED);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(e -> EventType.HEALTH_CHECK_FAILED.equals(e.getEventType())));
    }

    @Test
    void testFilterByTimeRange() throws InterruptedException {
        LocalDateTime start = LocalDateTime.now();

        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"));
        Thread.sleep(100);
        LocalDateTime middle = LocalDateTime.now();
        Thread.sleep(100);
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_SUCCESS, "service-b", "192.168.1.2", 8080, "Message 2"));
        Thread.sleep(100);
        LocalDateTime end = LocalDateTime.now();

        List<DiscoveryEvent> filtered = eventStore.getEventsByTimeRange(middle, end);

        assertEquals(1, filtered.size());
        assertEquals("service-b", filtered.get(0).getServiceId());
    }

    @Test
    void testCombinedFilters() throws InterruptedException {
        LocalDateTime start = LocalDateTime.now();

        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-a", "192.168.1.1", 8080, "Message 2"));
        Thread.sleep(100);
        LocalDateTime middle = LocalDateTime.now();
        Thread.sleep(100);
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-a", "192.168.1.1", 8080, "Message 3"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-b", "192.168.1.2", 8080, "Message 4"));
        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 5"));

        List<DiscoveryEvent> filtered = eventStore.getEventsWithFilters(
                "service-a",
                EventType.HEALTH_CHECK_FAILED,
                middle,
                null
        );

        assertEquals(1, filtered.size());
        assertEquals("service-a", filtered.get(0).getServiceId());
        assertEquals(EventType.HEALTH_CHECK_FAILED, filtered.get(0).getEventType());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        DiscoveryEvent event = new DiscoveryEvent(
                                EventType.DISCOVERED,
                                "service-" + threadId,
                                "192.168.1." + threadId,
                                8080,
                                "Thread " + threadId + " Event " + j
                        );
                        eventStore.addEvent(event);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        List<DiscoveryEvent> events = eventStore.getAllEvents();
        // Should have exactly max size due to circular buffer
        assertEquals(properties.getHistoryMaxSize(), events.size());
    }

    @Test
    void testEmptyStore() {
        List<DiscoveryEvent> events = eventStore.getAllEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    void testFilterWithNullReturnsAll() {
        eventStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"));
        eventStore.addEvent(new DiscoveryEvent(EventType.HEALTH_CHECK_SUCCESS, "service-b", "192.168.1.2", 8080, "Message 2"));

        List<DiscoveryEvent> byService = eventStore.getEventsByServiceId(null);
        List<DiscoveryEvent> byType = eventStore.getEventsByType(null);
        List<DiscoveryEvent> byTime = eventStore.getEventsByTimeRange(null, null);

        assertEquals(2, byService.size());
        assertEquals(2, byType.size());
        assertEquals(2, byTime.size());
    }

    @Test
    void testHistoryDisabled() {
        properties.setHistoryEnabled(false);
        DiscoveryEventStore disabledStore = new DiscoveryEventStore(properties);

        disabledStore.addEvent(new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message"));

        List<DiscoveryEvent> events = disabledStore.getAllEvents();
        assertTrue(events.isEmpty());
    }
}
