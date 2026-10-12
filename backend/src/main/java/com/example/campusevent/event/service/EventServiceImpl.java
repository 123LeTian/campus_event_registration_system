package com.example.campusevent.event.service;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.common.exception.BusinessException;
import com.example.campusevent.common.util.CurrentUserUtils;
import com.example.campusevent.event.dto.EventCreateRequest;
import com.example.campusevent.event.dto.EventQueryRequest;
import com.example.campusevent.event.dto.EventUpdateRequest;
import com.example.campusevent.event.entity.Event;
import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.event.vo.EventVO;
import com.example.campusevent.registration.mapper.RegistrationMapper;
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
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;

    public EventServiceImpl(EventMapper eventMapper, RegistrationMapper registrationMapper) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
    }

    @Override
    @Transactional
    public EventVO createEvent(EventCreateRequest request) {
        requireOrganizerOrAdmin();
        request.normalize();
        validateTimeRange(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        Event event = new Event();
        applyRequest(event, request);
        event.setOrganizerId(CurrentUserUtils.getCurrentUserId());
        event.setCurrentParticipants(0);
        event.setStatus(STATUS_PUBLISHED);
        event.setDeleted(false);
        eventMapper.insert(event);
        log.info("Event created: eventId={}, organizerId={}, title={}",
                event.getId(), event.getOrganizerId(), event.getTitle());
        return toVO(requiredEvent(event.getId(), false), false);
    }

    @Override
    @Transactional
    public EventVO updateEvent(Long id, EventUpdateRequest request) {
        requireOrganizerOrAdmin();
        request.normalize();
        validateTimeRange(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        Event event = requiredEvent(id, false);
        requireOwnerOrAdmin(event);
        if (!event.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动已结束，不能编辑", HttpStatus.CONFLICT);
        }
        if (request.getMaxParticipants() < event.getCurrentParticipants()) {
            throw new BusinessException(ErrorCode.CONFLICT, "人数上限不能小于当前报名人数", HttpStatus.CONFLICT);
        }
        if (event.getCurrentParticipants() > 0 && timeChanged(event, request)) {
            throw new BusinessException(ErrorCode.CONFLICT, "已有报名时不能修改活动时间", HttpStatus.CONFLICT);
        }

        applyRequest(event, request);
        eventMapper.updateById(event);
        log.info("Event updated: eventId={}, operatorId={}", id, CurrentUserUtils.getCurrentUserId());
        return toVO(requiredEvent(id, false), false);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        requireOrganizerOrAdmin();
        Event event = requiredEvent(id, false);
        requireOwnerOrAdmin(event);
        event.setDeleted(true);
        eventMapper.updateById(event);
        log.warn("Event deleted: eventId={}, operatorId={}", id, CurrentUserUtils.getCurrentUserId());
    }

    @Override
    public EventVO getEventDetail(Long id) {
        return toVO(requiredEvent(id, false), true);
    }

    @Override
    public PageResponse<EventVO> pageEvents(EventQueryRequest request) {
        long page = Math.max(request.getPage(), 1);
        long pageSize = Math.min(Math.max(request.getPageSize(), 1), 50);
        Long organizerId = resolveOrganizerFilter(request);
        String keyword = normalizeKeyword(request.getKeyword());
        String displayStatus = normalizeDisplayStatus(request.getStatus());
        boolean includeDeleted = includeDeleted(displayStatus);
        long total = eventMapper.countEvents(organizerId, keyword, displayStatus, includeDeleted);
        List<EventVO> records = eventMapper.pageEvents(organizerId, keyword, displayStatus, includeDeleted, pageSize, (page - 1) * pageSize)
                .stream()
                .map(event -> toVO(event, false))
                .toList();
        return new PageResponse<>(records, total, page, pageSize);
    }

    private Long resolveOrganizerFilter(EventQueryRequest request) {
        if (Boolean.TRUE.equals(request.getMine())) {
            if ("STUDENT".equals(CurrentUserUtils.getCurrentUserRole())) {
                log.warn("Forbidden event management list access: userId={}, action=mine_list",
                        CurrentUserUtils.getCurrentUserId());
                throw new BusinessException(ErrorCode.FORBIDDEN, "学生不能访问活动管理列表", HttpStatus.FORBIDDEN);
            }
            return CurrentUserUtils.getCurrentUserId();
        }
        // Phase 4 keeps the public event hall as a shared list; management pages pass mine=true.
        return null;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeDisplayStatus(String status) {
        if (status == null || status.trim().isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("REGISTRATION_OPEN".equals(normalized) || "FULL".equals(normalized) || "CLOSED".equals(normalized) || "FINISHED".equals(normalized)) {
            return normalized;
        }
        if ("DELETED".equals(normalized)) {
            if (!"ADMIN".equals(CurrentUserUtils.getCurrentUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看已删除活动", HttpStatus.FORBIDDEN);
            }
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "活动状态参数非法", HttpStatus.BAD_REQUEST);
    }

    private boolean includeDeleted(String displayStatus) {
        return "DELETED".equals(displayStatus) && "ADMIN".equals(CurrentUserUtils.getCurrentUserRole());
    }

    private void applyRequest(Event event, EventCreateRequest request) {
        event.setTitle(request.getTitle());
        event.setCopywriting(request.getCopywriting());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setMaxParticipants(request.getMaxParticipants());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束时间必须晚于开始时间", HttpStatus.BAD_REQUEST);
        }
        if (!registrationDeadline.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "报名截止时间必须早于开始时间", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean timeChanged(Event event, EventCreateRequest request) {
        return !event.getStartTime().equals(request.getStartTime())
                || !event.getEndTime().equals(request.getEndTime())
                || !event.getRegistrationDeadline().equals(request.getRegistrationDeadline());
    }

    private Event requiredEvent(Long id, boolean includeDeleted) {
        Event event = eventMapper.findDetailById(id);
        if (event == null || (!includeDeleted && Boolean.TRUE.equals(event.getDeleted()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
        }
        return event;
    }

    private void requireOrganizerOrAdmin() {
        String role = CurrentUserUtils.getCurrentUserRole();
        if (!"ORGANIZER".equals(role) && !"ADMIN".equals(role)) {
            log.warn("Forbidden event management access: userId={}, role={}",
                    CurrentUserUtils.getCurrentUserId(), role);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限管理活动", HttpStatus.FORBIDDEN);
        }
    }

    private void requireOwnerOrAdmin(Event event) {
        String role = CurrentUserUtils.getCurrentUserRole();
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (!"ADMIN".equals(role) && !event.getOrganizerId().equals(userId)) {
            log.warn("Forbidden event ownership access: userId={}, eventId={}, action=manage",
                    userId, event.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人活动", HttpStatus.FORBIDDEN);
        }
    }

    private EventVO toVO(Event event, boolean includeRegistration) {
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
                includeRegistration && registeredByCurrentUser(event.getId()),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private boolean registeredByCurrentUser(Long eventId) {
        String role = CurrentUserUtils.getCurrentUserRole();
        if (!"STUDENT".equals(role)) {
            return false;
        }
        return registrationMapper.countActiveByEventAndUser(eventId, CurrentUserUtils.getCurrentUserId()) > 0;
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
}
