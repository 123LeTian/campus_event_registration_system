package com.example.campusevent.event.service;

import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.event.dto.EventCreateRequest;
import com.example.campusevent.event.dto.EventQueryRequest;
import com.example.campusevent.event.dto.EventUpdateRequest;
import com.example.campusevent.event.vo.EventVO;

public interface EventService {
    EventVO createEvent(EventCreateRequest request);
    EventVO updateEvent(Long id, EventUpdateRequest request);
    void deleteEvent(Long id);
    EventVO getEventDetail(Long id);
    PageResponse<EventVO> pageEvents(EventQueryRequest request);
}
