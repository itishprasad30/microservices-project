package com.microservices.order.kafka;

import com.microservices.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "order-events";

    public void sendOrderEvent(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(TOPIC, event.getOrderNumber(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event sent successfully: {}", event.getOrderNumber());
            } else {
                log.error("Failed to send order event: {}", event.getOrderNumber(), ex);
            }
        });
    }
}
