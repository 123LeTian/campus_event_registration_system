package com.example.campusevent.common.security;

import com.example.campusevent.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
public class SecurityProbeController {

    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminCheck() {
        return ApiResponse.success("admin ok");
    }
}
