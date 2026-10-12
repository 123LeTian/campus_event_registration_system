package com.example.campusevent.registration.service.impl;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.common.exception.BusinessException;
import com.example.campusevent.common.util.CurrentUserUtils;
import com.example.campusevent.event.entity.Event;
import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.event.vo.EventVO;
import com.example.campusevent.registration.entity.Registration;
import com.example.campusevent.registration.mapper.RegistrationMapper;
import com.example.campusevent.registration.service.RegistrationService;
import com.example.campusevent.registration.vo.RegistrationVO;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;

    public RegistrationServiceImpl(EventMapper eventMapper, RegistrationMapper registrationMapper) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
    }

    @Override
    @Transactional
    public EventVO register(Long eventId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Event lockedEvent = requiredEventForUpdate(eventId);

        Registration existing = registrationMapper.findByEventAndUser(eventId, userId);
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            throw conflict("您已经报名该活动");
        }
        rejectUnavailableForRegistration(lockedEvent);

        int updated = eventMapper.increaseParticipantsIfAvailable(eventId);
        if (updated == 0) {
            Event latest = requiredEventForUpdate(eventId);
            rejectUnavailableForRegistration(latest);
            if (latest.getCurrentParticipants() >= latest.getMaxParticipants()) {
                throw conflict("活动名额已满");
            }
            throw conflict("活动暂时无法报名");
        }

        if (existing == null) {
            registrationMapper.insertActive(eventId, userId);
        } else if (registrationMapper.reactivateCancelled(eventId, userId) == 0) {
            throw conflict("您已经报名该活动");
        }

        assertParticipantConsistency(eventId);
        log.info("Registration succeeded: eventId={}, userId={}", eventId, userId);
        return toVO(requiredEventForUpdate(eventId), true);
    }

    @Override
    @Transactional
    public EventVO cancel(Long eventId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Event lockedEvent = requiredEventForUpdate(eventId);
        if (!lockedEvent.getEndTime().isAfter(LocalDateTime.now())) {
            throw conflict("活动已结束，不能取消报名");
        }

        int cancelled = registrationMapper.cancelActive(eventId, userId);
        if (cancelled == 0) {
            throw conflict("当前没有有效报名");
        }
        int decreased = eventMapper.decreaseParticipantsIfPositive(eventId);
        if (decreased == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "报名人数状态异常，请稍后重试", HttpStatus.CONFLICT);
        }

        assertParticipantConsistency(eventId);
        log.info("Registration cancelled: eventId={}, userId={}", eventId, userId);
        return toVO(requiredEventForUpdate(eventId), false);
    }

    @Override
    public PageResponse<RegistrationVO> pageEventRegistrations(Long eventId, Integer page, Integer pageSize, String status) {
        Event event = eventMapper.findDetailById(eventId);
        if (event == null || Boolean.TRUE.equals(event.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
        }
        String role = CurrentUserUtils.getCurrentUserRole();
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (!"ADMIN".equals(role) && (!"ORGANIZER".equals(role) || !event.getOrganizerId().equals(userId))) {
            log.warn("Forbidden registration list access: userId={}, eventId={}, action=view_registrations",
                    userId, eventId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该活动报名名单", HttpStatus.FORBIDDEN);
        }

        String normalizedStatus = normalizeStatus(status);
        long currentPage = normalizePage(page);
        long currentPageSize = normalizePageSize(pageSize);
        long total = registrationMapper.countEventRegistrations(eventId, normalizedStatus);
        List<RegistrationVO> records = registrationMapper.pageEventRegistrations(
                eventId,
                normalizedStatus,
                currentPageSize,
                (currentPage - 1) * currentPageSize,
                RegistrationMapper.REGISTRATION_VIEW_COLUMNS
        );
        return new PageResponse<>(records, total, currentPage, currentPageSize);
    }

    @Override
    public PageResponse<RegistrationVO> pageMyRegistrations(Integer page, Integer pageSize, String status) {
        if (!"STUDENT".equals(CurrentUserUtils.getCurrentUserRole())) {
            log.warn("Forbidden my registrations access: userId={}, role={}",
                    CurrentUserUtils.getCurrentUserId(), CurrentUserUtils.getCurrentUserRole());
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色没有我的报名", HttpStatus.FORBIDDEN);
        }
        String normalizedStatus = normalizeStatus(status);
        long currentPage = normalizePage(page);
        long currentPageSize = normalizePageSize(pageSize);
        Long userId = CurrentUserUtils.getCurrentUserId();
        long total = registrationMapper.countMyRegistrations(userId, normalizedStatus);
        List<RegistrationVO> records = registrationMapper.pageMyRegistrations(
                userId,
                normalizedStatus,
                currentPageSize,
                (currentPage - 1) * currentPageSize,
                RegistrationMapper.REGISTRATION_VIEW_COLUMNS
        );
        return new PageResponse<>(records, total, currentPage, currentPageSize);
    }

    private Event requiredEventForUpdate(Long eventId) {
        Event event = eventMapper.findDetailByIdForUpdate(eventId);
        if (event == null || Boolean.TRUE.equals(event.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
        }
        return event;
    }

    private void rejectUnavailableForRegistration(Event event) {
        LocalDateTime now = LocalDateTime.now();
        if (!event.getEndTime().isAfter(now)) {
            throw conflict("活动已结束，不能报名");
        }
        if (!event.getRegistrationDeadline().isAfter(now)) {
            throw conflict("报名已截止");
        }
        if (event.getCurrentParticipants() >= event.getMaxParticipants()) {
            throw conflict("活动名额已满");
        }
    }

    private void assertParticipantConsistency(Long eventId) {
        Event latest = eventMapper.findDetailByIdForUpdate(eventId);
        int activeCount = registrationMapper.countActiveByEventId(eventId);
        if (latest == null || latest.getCurrentParticipants() != activeCount) {
            throw new BusinessException(ErrorCode.CONFLICT, "报名人数状态异常，请稍后重试", HttpStatus.CONFLICT);
        }
    }

    private EventVO toVO(Event event, boolean registeredByCurrentUser) {
        int remainingParticipants = Math.max(event.getMaxParticipants() - event.getCurrentParticipants(), 0);
        BigDecimal registrationRate = BigDecimal.ZERO;
        if (event.getMaxParticipants() > 0) {
            registrationRate = BigDecimal.valueOf(event.getCurrentParticipants())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(event.getMaxParticipants()), 2, RoundingMode.HALF_UP);
        }
        return new EventVO(
                event.getId(),
                event.getTitle(),
                event.getCopywriting(),
                event.getDescription(),
                event.getLocation(),
                event.getOrganizerId(),
                event.getOrganizerName(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRegistrationDeadline(),
                event.getMaxParticipants(),
                event.getCurrentParticipants(),
                remainingParticipants,
                registrationRate,
                event.getStatus(),
                displayStatus(event),
                registeredByCurrentUser,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private String displayStatus(Event event) {
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(event.getDeleted())) {
            return "DELETED";
        }
        if (!event.getEndTime().isAfter(now)) {
            return "FINISHED";
        }
        if (!event.getRegistrationDeadline().isAfter(now)) {
            return "CLOSED";
        }
        if (event.getCurrentParticipants() >= event.getMaxParticipants()) {
            return "FULL";
        }
        return "REGISTRATION_OPEN";
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message, HttpStatus.CONFLICT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        if ("ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized) || "CANCELLED".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "报名状态参数非法", HttpStatus.BAD_REQUEST);
    }

    private long normalizePage(Integer page) {
        return Math.max(page == null ? 1 : page, 1);
    }

    private long normalizePageSize(Integer pageSize) {
        return Math.min(Math.max(pageSize == null ? 10 : pageSize, 1), 50);
    }
}
