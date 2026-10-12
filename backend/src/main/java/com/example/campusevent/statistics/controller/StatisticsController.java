package com.example.campusevent.statistics.controller;

import com.example.campusevent.common.api.ApiResponse;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.statistics.service.StatisticsService;
import com.example.campusevent.statistics.vo.EventStatisticsVO;
import com.example.campusevent.statistics.vo.StatisticsOverviewVO;
import com.example.campusevent.statistics.vo.StatusDistributionVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<StatisticsOverviewVO> overview() {
        return ApiResponse.success(statisticsService.overview());
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<PageResponse<EventStatisticsVO>> events(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(statisticsService.pageEvents(page, pageSize));
    }

    @GetMapping("/status-distribution")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<List<StatusDistributionVO>> statusDistribution() {
        return ApiResponse.success(statisticsService.statusDistribution());
    }
}
