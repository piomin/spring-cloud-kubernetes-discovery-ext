package org.springframework.cloud.kubernetes.discovery.ext.watcher.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.DiscoveryEvent;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.model.EventType;
import org.springframework.cloud.kubernetes.discovery.ext.watcher.service.DiscoveryEventStore;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DiscoveryHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DiscoveryEventStore eventStore;

    private List<DiscoveryEvent> testEvents;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DiscoveryHistoryController controller = new DiscoveryHistoryController(eventStore);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        testEvents = Arrays.asList(
                new DiscoveryEvent(EventType.DISCOVERED, "service-a", "192.168.1.1", 8080, "Message 1"),
                new DiscoveryEvent(EventType.HEALTH_CHECK_SUCCESS, "service-a", "192.168.1.1", 8080, "Message 2"),
                new DiscoveryEvent(EventType.HEALTH_CHECK_FAILED, "service-b", "192.168.1.2", 8080, "Message 3"),
                new DiscoveryEvent(EventType.DEREGISTRATION_STARTED, "service-b", "192.168.1.2", 8080, "Message 4"),
                new DiscoveryEvent(EventType.DEREGISTRATION_COMPLETED, "service-b", "192.168.1.2", 8080, "Message 5")
        );
    }

    @Test
    void testGetAllHistory() throws Exception {
        when(eventStore.getEventsWithFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(testEvents);

        mockMvc.perform(get("/api/discovery/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].eventType").value("DEREGISTRATION_COMPLETED"))
                .andExpect(jsonPath("$[4].eventType").value("DISCOVERED"));
    }

    @Test
    void testGetHistoryByServiceId() throws Exception {
        List<DiscoveryEvent> filteredEvents = Arrays.asList(
                testEvents.get(0),
                testEvents.get(1)
        );

        when(eventStore.getEventsWithFilters(eq("service-a"), isNull(), isNull(), isNull()))
                .thenReturn(filteredEvents);

        mockMvc.perform(get("/api/discovery/history")
                        .param("serviceId", "service-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].serviceId").value("service-a"))
                .andExpect(jsonPath("$[1].serviceId").value("service-a"));
    }

    @Test
    void testGetHistoryByEventType() throws Exception {
        List<DiscoveryEvent> filteredEvents = List.of(testEvents.get(2));

        when(eventStore.getEventsWithFilters(isNull(), eq(EventType.HEALTH_CHECK_FAILED), isNull(), isNull()))
                .thenReturn(filteredEvents);

        mockMvc.perform(get("/api/discovery/history")
                        .param("eventType", "HEALTH_CHECK_FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("HEALTH_CHECK_FAILED"));
    }

    @Test
    void testGetHistoryByTimeRange() throws Exception {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        when(eventStore.getEventsWithFilters(isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testEvents);

        mockMvc.perform(get("/api/discovery/history")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void testGetHistoryWithPagination() throws Exception {
        when(eventStore.getEventsWithFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(testEvents);

        // First page with size 2
        mockMvc.perform(get("/api/discovery/history")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Second page with size 2
        mockMvc.perform(get("/api/discovery/history")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Third page with size 2 (only 1 item left)
        mockMvc.perform(get("/api/discovery/history")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testGetHistoryWithInvalidEventType() throws Exception {
        mockMvc.perform(get("/api/discovery/history")
                        .param("eventType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid eventType")));
    }

    @Test
    void testGetHistoryEmptyResult() throws Exception {
        when(eventStore.getEventsWithFilters(anyString(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/discovery/history")
                        .param("serviceId", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetHistoryPageOutOfRange() throws Exception {
        when(eventStore.getEventsWithFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(testEvents);

        mockMvc.perform(get("/api/discovery/history")
                        .param("page", "100")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetHistoryWithInvalidPaginationParams() throws Exception {
        mockMvc.perform(get("/api/discovery/history")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page must be >= 0")));

        mockMvc.perform(get("/api/discovery/history")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Size must be >= 1")));
    }

    @Test
    void testGetHistoryWithCombinedFilters() throws Exception {
        List<DiscoveryEvent> filteredEvents = List.of(testEvents.get(1));

        when(eventStore.getEventsWithFilters(eq("service-a"), eq(EventType.HEALTH_CHECK_SUCCESS), any(), any()))
                .thenReturn(filteredEvents);

        mockMvc.perform(get("/api/discovery/history")
                        .param("serviceId", "service-a")
                        .param("eventType", "HEALTH_CHECK_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceId").value("service-a"))
                .andExpect(jsonPath("$[0].eventType").value("HEALTH_CHECK_SUCCESS"));
    }
}
