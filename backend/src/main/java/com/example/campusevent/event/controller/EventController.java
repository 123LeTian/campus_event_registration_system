package com.example.campusevent.event.controller;

import com.example.campusevent.common.api.ApiResponse;
import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.event.dto.EventCreateRequest;
import com.example.campusevent.event.dto.EventQueryRequest;
import com.example.campusevent.event.dto.EventUpdateRequest;
import com.example.campusevent.event.service.EventService;
import com.example.campusevent.event.vo.EventVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ApiResponse<PageResponse<EventVO>> pageEvents(EventQueryRequest request) {
        return ApiResponse.success(eventService.pageEvents(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventVO> detail(@PathVariable Long id) {
        return ApiResponse.success(eventService.getEventDetail(id));
    }

    @PostMapping
    public ApiResponse<EventVO> create(@Valid @RequestBody EventCreateRequest request) {
        return ApiResponse.success("活动创建成功", eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EventVO> update(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request) {
        return ApiResponse.success("活动更新成功", eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ApiResponse.success("活动删除成功", null);
    }
}
