package com.example.campusevent.registration.controller;

import com.example.campusevent.common.api.ApiResponse;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.event.vo.EventVO;
import com.example.campusevent.registration.service.RegistrationService;
import com.example.campusevent.registration.vo.RegistrationVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<EventVO> register(@PathVariable Long eventId) {
        return ApiResponse.success("报名成功", registrationService.register(eventId));
    }

    @DeleteMapping("/events/{eventId}/registrations/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<EventVO> cancel(@PathVariable Long eventId) {
        return ApiResponse.success("取消报名成功", registrationService.cancel(eventId));
    }

    @GetMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<PageResponse<RegistrationVO>> pageEventRegistrations(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        return ApiResponse.success(registrationService.pageEventRegistrations(eventId, page, pageSize, status));
    }

    @GetMapping("/my/registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<RegistrationVO>> pageMyRegistrations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        return ApiResponse.success(registrationService.pageMyRegistrations(page, pageSize, status));
    }
}
