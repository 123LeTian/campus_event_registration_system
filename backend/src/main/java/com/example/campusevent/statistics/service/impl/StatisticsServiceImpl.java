package com.example.campusevent.statistics.service.impl;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.common.exception.BusinessException;
import com.example.campusevent.common.util.CurrentUserUtils;
import com.example.campusevent.statistics.mapper.StatisticsMapper;
import com.example.campusevent.statistics.service.StatisticsService;
import com.example.campusevent.statistics.vo.EventStatisticsVO;
import com.example.campusevent.statistics.vo.StatisticsOverviewVO;
import com.example.campusevent.statistics.vo.StatusDistributionVO;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);
    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();

    static {
        STATUS_LABELS.put("REGISTRATION_OPEN", "报名中");
        STATUS_LABELS.put("FULL", "已满员");
        STATUS_LABELS.put("CLOSED", "报名截止");
        STATUS_LABELS.put("FINISHED", "已结束");
    }

    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public StatisticsOverviewVO overview() {
        Long organizerId = resolveOrganizerScope();
        return new StatisticsOverviewVO(
                statisticsMapper.countEvents(organizerId),
                statisticsMapper.countActiveRegistrations(organizerId),
                statisticsMapper.countEventsByDisplayStatus(organizerId, "REGISTRATION_OPEN"),
                statisticsMapper.countEventsByDisplayStatus(organizerId, "FINISHED")
        );
    }

    @Override
    public PageResponse<EventStatisticsVO> pageEvents(Integer page, Integer pageSize) {
        Long organizerId = resolveOrganizerScope();
        long currentPage = Math.max(page == null ? 1 : page, 1);
        long currentPageSize = Math.min(Math.max(pageSize == null ? 10 : pageSize, 1), 50);
        long total = statisticsMapper.countEvents(organizerId);
        List<EventStatisticsVO> records = statisticsMapper.pageEventStatistics(
                organizerId,
                currentPageSize,
                (currentPage - 1) * currentPageSize
        );
        return new PageResponse<>(records, total, currentPage, currentPageSize);
    }

    @Override
    public List<StatusDistributionVO> statusDistribution() {
        Long organizerId = resolveOrganizerScope();
        Map<String, Long> counts = statisticsMapper.statusDistribution(organizerId).stream()
                .collect(Collectors.toMap(StatusDistributionVO::getStatus, StatusDistributionVO::getCount));
        return STATUS_LABELS.entrySet().stream()
                .map(entry -> new StatusDistributionVO(entry.getKey(), entry.getValue(), counts.getOrDefault(entry.getKey(), 0L)))
                .toList();
    }

    private Long resolveOrganizerScope() {
        String role = CurrentUserUtils.getCurrentUserRole();
        if ("ADMIN".equals(role)) {
            return null;
        }
        if ("ORGANIZER".equals(role)) {
            return CurrentUserUtils.getCurrentUserId();
        }
        log.warn("Forbidden statistics access: userId={}, role={}",
                CurrentUserUtils.getCurrentUserId(), role);
        throw new BusinessException(ErrorCode.FORBIDDEN, "学生不能访问统计接口", HttpStatus.FORBIDDEN);
    }
}
