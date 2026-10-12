package com.example.campusevent.statistics.vo;

public record StatisticsOverviewVO(
        long totalEvents,
        long totalRegistrations,
        long openEvents,
        long finishedEvents
) {
}
