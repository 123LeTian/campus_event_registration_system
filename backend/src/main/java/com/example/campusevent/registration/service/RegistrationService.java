package com.example.campusevent.registration.service;

import com.example.campusevent.common.api.PageResponse;
import com.example.campusevent.event.vo.EventVO;
import com.example.campusevent.registration.vo.RegistrationVO;

public interface RegistrationService {

    EventVO register(Long eventId);

    EventVO cancel(Long eventId);

    PageResponse<RegistrationVO> pageEventRegistrations(Long eventId, Integer page, Integer pageSize, String status);

    PageResponse<RegistrationVO> pageMyRegistrations(Integer page, Integer pageSize, String status);
}
