package com.example.campusevent.ai.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiGenerateVO(
        String introduction,
        String promotion,
        List<String> highlights,
        String audience
) {

    public static AiGenerateVO introduction(String introduction, List<String> highlights, String audience) {
        return new AiGenerateVO(introduction, null, highlights, audience);
    }

    public static AiGenerateVO copyAndIntroduction(String promotion, String introduction, List<String> highlights, String audience) {
        return new AiGenerateVO(introduction, promotion, highlights, audience);
    }

    public static AiGenerateVO promotion(String promotion, List<String> highlights, String audience) {
        return new AiGenerateVO(null, promotion, highlights, audience);
    }
}
