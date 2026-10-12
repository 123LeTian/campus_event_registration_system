package com.example.campusevent.ai.controller;

import com.example.campusevent.ai.dto.EventIntroductionRequest;
import com.example.campusevent.ai.dto.EventPromotionRequest;
import com.example.campusevent.ai.service.AiService;
import com.example.campusevent.ai.vo.AiGenerateVO;
import com.example.campusevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/event-introduction")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<AiGenerateVO> generateEventIntroduction(@Valid @RequestBody EventIntroductionRequest request) {
        return ApiResponse.success(aiService.generateEventIntroduction(request));
    }

    @PostMapping("/event-promotion")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<AiGenerateVO> generateEventPromotion(@Valid @RequestBody EventPromotionRequest request) {
        return ApiResponse.success(aiService.generateEventPromotion(request));
    }
}
