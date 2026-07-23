package com.microservices.notification.service;

import com.microservices.notification.dto.NotificationRequest;
import com.microservices.notification.dto.NotificationResponse;
import com.microservices.notification.entity.Notification;
import com.microservices.notification.exception.ResourceNotFoundException;
import com.microservices.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;  // Added 'final'

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        log.info("Creating notification for user: {}", request.getUserId());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .relatedOrderId(request.getRelatedOrderId())
                .relatedOrderNumber(request.getRelatedOrderNumber())
                .status(Notification.NotificationStatus.PENDING)
                .build();

        Notification savedNotification = notificationRepository.save(notification);  // Fixed variable name
        log.info("Notification created with Id: {}", savedNotification.getId());

        // In real implementation, send notification via email/SMS/push
        sendNotification(savedNotification);

        return mapToResponse(savedNotification);
    }

    @Cacheable(value = "notifications", key = "#id")
    public NotificationResponse getNotificationById(Long id) {
        log.info("Fetching notification by ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));
        return mapToResponse(notification);
    }

    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnsentNotifications(Long userId) {
        log.info("Fetching unsent notifications for user: {}", userId);
        return notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationAsSent(Long id) {
        log.info("Marking notification as sent: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification marked as sent: {}", id);
    }

    private void sendNotification(Notification notification) {
        // Simulate sending notification
        log.info("Sending notification - User: {}, Title: {}", notification.getUserId(), notification.getTitle());

        // In real implementation:
        // - Send email via SMTP
        // - Send SMS via Twilio
        // - Send push notification via Firebase

        // For demo, mark as sent immediately
        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification sent: {}", notification.getId());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .status(notification.getStatus().name())
                .relatedOrderId(notification.getRelatedOrderId())
                .relatedOrderNumber(notification.getRelatedOrderNumber())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}