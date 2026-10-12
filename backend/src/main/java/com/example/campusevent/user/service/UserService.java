package com.example.campusevent.user.service;

import com.example.campusevent.user.dto.LoginRequest;
import com.example.campusevent.user.dto.RegisterRequest;
import com.example.campusevent.user.entity.User;
import com.example.campusevent.user.vo.LoginVO;
import com.example.campusevent.user.vo.UserVO;

public interface UserService {

    UserVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    UserVO getCurrentUser(Long userId);

    User findEnabledUserById(Long userId);

    static UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }
}
