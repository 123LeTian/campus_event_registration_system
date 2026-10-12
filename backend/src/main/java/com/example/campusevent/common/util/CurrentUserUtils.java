package com.example.campusevent.common.util;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.exception.BusinessException;
import com.example.campusevent.common.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserUtils {

    private CurrentUserUtils() {
    }

    public static Long getCurrentUserId() {
        return currentUser().userId();
    }

    public static String getCurrentUsername() {
        return currentUser().username();
    }

    public static String getCurrentUserRole() {
        return currentUser().role();
    }

    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }
}
