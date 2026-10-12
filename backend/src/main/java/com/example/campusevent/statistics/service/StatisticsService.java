package com.example.campusevent.statistics.service;

import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.statistics.vo.EventStatisticsVO;
import com.example.campusevent.statistics.vo.StatisticsOverviewVO;
import com.example.campusevent.statistics.vo.StatusDistributionVO;

import java.util.List;

public interface StatisticsService {

    StatisticsOverviewVO overview();

    PageResponse<EventStatisticsVO> pageEvents(Integer page, Integer pageSize);

    List<StatusDistributionVO> statusDistribution();
}
