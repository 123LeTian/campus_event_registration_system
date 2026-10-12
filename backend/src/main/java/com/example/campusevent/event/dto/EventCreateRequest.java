package com.example.campusevent.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class EventCreateRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100 个字符")
    private String title;

    @NotBlank(message = "活动文案不能为空")
    @Size(max = 300, message = "活动文案长度不能超过 300 个字符")
    private String copywriting;

    @NotBlank(message = "活动介绍不能为空")
    private String description;

    @NotBlank(message = "地点不能为空")
    @Size(max = 100, message = "地点长度不能超过 100 个字符")
    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "人数上限不能为空")
    @Min(value = 1, message = "人数上限必须大于 0")
    private Integer maxParticipants;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCopywriting() { return copywriting; }
    public void setCopywriting(String copywriting) { this.copywriting = copywriting; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public void normalize() {
        if (title != null) { title = title.trim(); }
        if (copywriting != null) { copywriting = copywriting.trim(); }
        if (description != null) { description = description.trim(); }
        if (location != null) { location = location.trim(); }
    }
}
