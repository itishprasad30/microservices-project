package com.microservices.order.service;
import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.OrderItem;
import com.microservices.order.event.OrderEvent;
import com.microservices.order.exception.ResourceNotFoundException;
import com.microservices.order.kafka.OrderEventProducer;
import com.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .userId(request.getUserId())
                .orderNumber(orderNumber)
                .shippingAddress(request.getShippingAddress())
                .status(Order.OrderStatus.PENDING)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            BigDecimal unitPrice = getProductPrice(itemRequest.getProductId());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            order.addItem(item);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}, Order Number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        // Send Kafka event
        sendOrderEvent(savedOrder, OrderEvent.EventType.CREATED);

        return mapToResponse(savedOrder);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return mapToResponse(order);
    }

    @Cacheable(value = "orders", key = "#orderNumber")
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.info("Fetching order by order number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Fetching orders for user: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status) {
        log.info("Updating order status: {} for order ID: {}", status, id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated for ID: {}", updatedOrder.getId());

        // Send Kafka event
        OrderEvent.EventType eventType = convertStatusToEventType(status);
        sendOrderEvent(updatedOrder, eventType);

        return mapToResponse(updatedOrder);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    public void cancelOrder(Long id) {
        log.info("Cancelling order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed order");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled with ID: {}", id);

        // Send Kafka event
        sendOrderEvent(order, OrderEvent.EventType.CANCELLED);
    }

    private void sendOrderEvent(Order order, OrderEvent.EventType eventType) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();

        orderEventProducer.sendOrderEvent(event);
        log.info("Order event sent: {}", event);
    }

    private BigDecimal getProductPrice(Long productId) {
        // In real implementation, call product service
        return BigDecimal.valueOf(100.00);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderEvent.EventType convertStatusToEventType(Order.OrderStatus status) {
        return switch (status) {
            case COMPLETED -> OrderEvent.EventType.COMPLETED;
            case CANCELLED -> OrderEvent.EventType.CANCELLED;
            case FAILED -> OrderEvent.EventType.FAILED;
            default -> OrderEvent.EventType.UPDATED;
        };
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}