package com.adl.et.telco.crm.usermanagerservice.client;

import com.adl.et.telco.crm.usermanagerservice.dto.common.DBWriteRequestMySQL;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes MySQL DB-write events to the db-write-service over Kafka.
 * Fire-and-forget — no reply expected.
 */
@Service
@Slf4j
public class MySQLKafkaEventPublisher {

    private final KafkaTemplate<String, Object> mysqlKafkaTemplate;

    @Value("${kafka.mysql.publish-topic}")
    private String publishTopic;

    public MySQLKafkaEventPublisher(KafkaTemplate<String, Object> mysqlKafkaTemplate) {
        this.mysqlKafkaTemplate = mysqlKafkaTemplate;
    }

    /**
     * Publishes a DB-write event to Kafka. Fire-and-forget.
     *
     * @throws Exception if the broker is unreachable (send future fails)
     */
    public void publish(DBWriteRequestMySQL request) throws Exception {
        String key = request.getUserName();
        ProducerRecord<String, Object> record = new ProducerRecord<>(publishTopic, key, request);

        mysqlKafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[MySQL] Publish failed (key={}): {}", key, ex.getMessage());
            } else {
                log.debug("[MySQL] Published: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}