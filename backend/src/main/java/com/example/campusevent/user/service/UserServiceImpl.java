package com.example.campusevent.user.service;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.exception.BusinessException;
import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.user.dto.LoginRequest;
import com.example.campusevent.user.dto.RegisterRequest;
import com.example.campusevent.user.entity.User;
import com.example.campusevent.user.entity.UserRole;
import com.example.campusevent.user.mapper.UserMapper;
import com.example.campusevent.user.vo.LoginVO;
import com.example.campusevent.user.vo.UserVO;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userMapper.countByUsername(username) > 0) {
            log.warn("Registration rejected because username already exists: username={}", username);
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setRole(UserRole.STUDENT.name());
        user.setEnabled(true);
        userMapper.insert(user);
        log.info("User registered: userId={}, username={}", user.getId(), user.getUsername());
        return UserService.toVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername().trim());
        if (user == null) {
            log.warn("Login failed: username={}, reason=user_not_found", request.getUsername());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户不存在", HttpStatus.BAD_REQUEST);
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            log.warn("Login failed: username={}, reason=user_disabled", request.getUsername());
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用", HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: username={}, reason=invalid_password", request.getUsername());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "密码错误", HttpStatus.BAD_REQUEST);
        }
        String token = jwtTokenProvider.createToken(user);
        log.info("Login succeeded: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, UserService.toVO(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        return UserService.toVO(findEnabledUserById(userId));
    }

    @Override
    public User findEnabledUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在", HttpStatus.NOT_FOUND);
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
