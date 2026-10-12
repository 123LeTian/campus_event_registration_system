package com.example.campusevent.ai.service;

import com.example.campusevent.ai.dto.EventIntroductionRequest;
import com.example.campusevent.ai.dto.EventPromotionRequest;
import com.example.campusevent.ai.vo.AiGenerateVO;

public interface AiService {

    AiGenerateVO generateEventIntroduction(EventIntroductionRequest request);

    AiGenerateVO generateEventPromotion(EventPromotionRequest request);
}
