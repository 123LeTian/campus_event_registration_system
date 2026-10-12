package com.example.campusevent.event.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventVO(
        Long id,
        String title,
        String copywriting,
        String description,
        String location,
        Long organizerId,
        String organizerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime registrationDeadline,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer remainingParticipants,
        BigDecimal registrationRate,
        String status,
        String displayStatus,
        Boolean registeredByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
