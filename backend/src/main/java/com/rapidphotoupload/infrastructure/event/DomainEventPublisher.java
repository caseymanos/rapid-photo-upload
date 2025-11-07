package com.rapidphotoupload.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidphotoupload.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Publisher for domain events to AWS EventBridge.
 * Converts domain events to EventBridge events and publishes them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisher {
    
    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.eventbridge.bus-name:default}")
    private String eventBusName;
    
    private static final String EVENT_SOURCE = "com.rapidphotoupload";
    
    /**
     * Publish a single domain event.
     * @param event the domain event to publish
     */
    public void publish(DomainEvent event) {
        publishAll(List.of(event));
    }
    
    /**
     * Publish multiple domain events in a batch.
     * @param events the list of domain events to publish
     */
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("No events to publish");
            return;
        }
        
        try {
            List<PutEventsRequestEntry> entries = events.stream()
                .map(this::toEventBridgeEntry)
                .collect(Collectors.toList());
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entries)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish {} events", response.failedEntryCount());
                response.entries().forEach(entry -> {
                    if (entry.errorCode() != null) {
                        log.error("Event failed: {} - {}", entry.errorCode(), entry.errorMessage());
                    }
                });
            } else {
                log.info("Successfully published {} events", events.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to publish domain events", e);
            // Don't throw - event publishing should not break the main flow
        }
    }
    
    private PutEventsRequestEntry toEventBridgeEntry(DomainEvent event) {
        try {
            String eventDetail = objectMapper.writeValueAsString(event);
            
            return PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source(EVENT_SOURCE)
                .detailType(event.getEventType())
                .detail(eventDetail)
                .build();
                
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
