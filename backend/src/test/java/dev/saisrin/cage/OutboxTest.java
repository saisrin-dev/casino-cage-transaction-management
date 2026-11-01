package dev.saisrin.cage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class OutboxTest {
  @Test
  void successfulPublishMarksDeliveryAfterBrokerAck() {
    OutboxRepository repo = mock(OutboxRepository.class);
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    OutboxEvent e = new OutboxEvent();
    e.id = 1L;
    e.aggregateId = 20L;
    e.payload = "synthetic";
    when(repo.findTop50ByPublishedAtIsNullOrderById()).thenReturn(List.of(e));
    when(kafka.send("cage.transactions.v1", "20", "synthetic"))
        .thenReturn(CompletableFuture.completedFuture(null));
    new OutboxPublisher(repo, kafka).publish();
    assertNotNull(e.publishedAt);
    verify(repo).save(e);
  }

  @Test
  void brokerFailureLeavesEventAvailableForRetry() {
    OutboxRepository repo = mock(OutboxRepository.class);
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    OutboxEvent e = new OutboxEvent();
    e.aggregateId = 20L;
    e.payload = "synthetic";
    when(repo.findTop50ByPublishedAtIsNullOrderById()).thenReturn(List.of(e));
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
    new OutboxPublisher(repo, kafka).publish();
    assertNull(e.publishedAt);
    assertEquals(1, e.attempts);
  }

  @Test
  void productionAdapterFailsClosed() {
    assertThrows(
        IllegalStateException.class,
        () -> new ProductionFilingAdapter().submit(new FilingBatch(), "ACCEPT"));
  }
}
