package com.example.campusevent.statistics.vo;

import java.math.BigDecimal;

public class EventStatisticsVO {

    private Long eventId;
    private String title;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer remainingParticipants;
    private BigDecimal registrationRate;
    private String displayStatus;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public Integer getRemainingParticipants() {
        return remainingParticipants;
    }

    public void setRemainingParticipants(Integer remainingParticipants) {
        this.remainingParticipants = remainingParticipants;
    }

    public BigDecimal getRegistrationRate() {
        return registrationRate;
    }

    public void setRegistrationRate(BigDecimal registrationRate) {
        this.registrationRate = registrationRate;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }
}
