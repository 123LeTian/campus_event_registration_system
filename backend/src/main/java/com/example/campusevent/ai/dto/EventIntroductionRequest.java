package com.example.campusevent.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class EventIntroductionRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100 个字符")
    private String title;

    @NotBlank(message = "地点不能为空")
    @Size(max = 100, message = "地点长度不能超过 100 个字符")
    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @NotBlank(message = "请填写适合人群")
    @Size(max = 100, message = "适合人群长度不能超过 100 个字符")
    private String audience;

    @NotBlank(message = "请填写活动类型")
    @Size(max = 100, message = "活动类型长度不能超过 100 个字符")
    private String activityType;

    @NotBlank(message = "请填写关键词")
    @Size(max = 300, message = "关键词长度不能超过 300 个字符")
    private String keywords;

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
}
