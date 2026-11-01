package dev.saisrin.cage;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Entity
class OutboxEvent extends Row {
  public Long aggregateId;

  @Column(columnDefinition = "text")
  public String payload;

  public Instant createdAt = Instant.now();
  public Instant publishedAt;
  public int attempts;
}

interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findTop50ByPublishedAtIsNullOrderById();
}

@Configuration
@EnableScheduling
class Scheduling {}

@Component
@ConditionalOnProperty(name = "cage.kafka-enabled", havingValue = "true")
class OutboxPublisher {
  private final OutboxRepository events;
  private final KafkaTemplate<String, String> kafka;

  OutboxPublisher(OutboxRepository e, KafkaTemplate<String, String> k) {
    events = e;
    kafka = k;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publish() {
    for (OutboxEvent e : events.findTop50ByPublishedAtIsNullOrderById()) {
      try {
        kafka
            .send("cage.transactions.v1", e.aggregateId.toString(), e.payload)
            .get(5, java.util.concurrent.TimeUnit.SECONDS);
        e.publishedAt = Instant.now();
      } catch (Exception ex) {
        e.attempts++;
      }
      events.save(e);
    }
  }
}
