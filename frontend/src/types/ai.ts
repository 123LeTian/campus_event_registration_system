export interface AiEventContext {
  title: string
  location: string
  startTime?: string
  endTime?: string
  audience?: string
  activityType?: string
  keywords?: string
}

export interface EventPromotionContext extends AiEventContext {
  description?: string
}

export interface EventIntroductionContext {
  title: string
  location: string
  activityType: string
  audience: string
  keywords: string
}

export interface AiGenerateVO {
  introduction?: string
  promotion?: string
  highlights: string[]
  audience: string
}
