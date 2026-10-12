import request from '@/api/request'
import type { EventIntroductionContext, AiGenerateVO, EventPromotionContext } from '@/types/ai'

export function generateEventIntroduction(data: EventIntroductionContext) {
  return request.post<unknown, AiGenerateVO>('/ai/event-introduction', data)
}

export function generateEventPromotion(data: EventPromotionContext) {
  return request.post<unknown, AiGenerateVO>('/ai/event-promotion', data)
}
