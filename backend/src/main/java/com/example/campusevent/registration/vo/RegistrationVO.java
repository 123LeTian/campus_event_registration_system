package com.example.campusevent.registration.vo;

import java.time.LocalDateTime;

public class RegistrationVO {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventLocation;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private String eventDisplayStatus;
    private Long userId;
    private String studentUsername;
    private String studentName;
    private String status;
    private LocalDateTime registrationTime;
    private LocalDateTime cancelledTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }
    public LocalDateTime getEventStartTime() { return eventStartTime; }
    public void setEventStartTime(LocalDateTime eventStartTime) { this.eventStartTime = eventStartTime; }
    public LocalDateTime getEventEndTime() { return eventEndTime; }
    public void setEventEndTime(LocalDateTime eventEndTime) { this.eventEndTime = eventEndTime; }
    public String getEventDisplayStatus() { return eventDisplayStatus; }
    public void setEventDisplayStatus(String eventDisplayStatus) { this.eventDisplayStatus = eventDisplayStatus; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRegistrationTime() { return registrationTime; }
    public void setRegistrationTime(LocalDateTime registrationTime) { this.registrationTime = registrationTime; }
    public LocalDateTime getCancelledTime() { return cancelledTime; }
    public void setCancelledTime(LocalDateTime cancelledTime) { this.cancelledTime = cancelledTime; }
}
