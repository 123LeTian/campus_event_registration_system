package com.example.campusevent.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class EventPromotionRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100 个字符")
    private String title;

    @NotBlank(message = "地点不能为空")
    @Size(max = 100, message = "地点长度不能超过 100 个字符")
    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Size(max = 100, message = "适合人群长度不能超过 100 个字符")
    private String audience;

    @Size(max = 100, message = "活动类型长度不能超过 100 个字符")
    private String activityType;

    @Size(max = 300, message = "关键词长度不能超过 300 个字符")
    private String keywords;

    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
