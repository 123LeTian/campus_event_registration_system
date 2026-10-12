package com.example.campusevent.user.controller;

import com.example.campusevent.common.api.ApiResponse;
import com.example.campusevent.common.util.CurrentUserUtils;
import com.example.campusevent.user.service.UserService;
import com.example.campusevent.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.success(userService.getCurrentUser(CurrentUserUtils.getCurrentUserId()));
    }
}
