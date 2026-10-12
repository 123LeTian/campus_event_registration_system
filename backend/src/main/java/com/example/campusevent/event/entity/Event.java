package com.example.campusevent.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;

import com.baomidou.mybatisplus.annotation.TableField;

import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("event")

public class Event {

    @TableId(type = IdType.AUTO)

    private Long id;

    private String title;

    private String copywriting;

    private String description;

    private String location;

    @TableField("organizer_id")

    private Long organizerId;

    @TableField("start_time")

    private LocalDateTime startTime;

    @TableField("end_time")

    private LocalDateTime endTime;

    @TableField("registration_deadline")

    private LocalDateTime registrationDeadline;

    @TableField("max_participants")

    private Integer maxParticipants;

    @TableField("current_participants")

    private Integer currentParticipants;

    private String status;

    private Boolean deleted;

    @TableField("created_at")

    private LocalDateTime createdAt;

    @TableField("updated_at")

    private LocalDateTime updatedAt;

    @TableField(exist = false)

    private String organizerName;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getCopywriting() { return copywriting; }

    public void setCopywriting(String copywriting) { this.copywriting = copywriting; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public Long getOrganizerId() { return organizerId; }

    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    public LocalDateTime getStartTime() { return startTime; }

    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public Integer getMaxParticipants() { return maxParticipants; }

    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Integer getCurrentParticipants() { return currentParticipants; }

    public void setCurrentParticipants(Integer currentParticipants) { this.currentParticipants = currentParticipants; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Boolean getDeleted() { return deleted; }

    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getOrganizerName() { return organizerName; }

    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
}
