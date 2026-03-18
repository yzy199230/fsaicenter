package com.fsa.aicenter.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fsa.aicenter.application.dto.request.LoginRequest;
import com.fsa.aicenter.application.dto.response.LoginResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.admin.aggregate.AdminUser;
import com.fsa.aicenter.domain.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private AdminUser testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new AdminUser();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRealName("管理员");
        testUser.setEnabled(true);

        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("登录成功")
    void testLogin_Success() {
        // Given
        String ip = "192.168.1.1";
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.getTokenName()).thenReturn("Authorization");
            stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("mock-token-value");

            // When
            LoginResponse response = authService.login(loginRequest, ip);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getUserId());
            assertEquals("admin", response.getUsername());
            assertEquals("管理员", response.getRealName());
            assertEquals("Authorization", response.getTokenName());
            assertEquals("mock-token-value", response.getTokenValue());

            verify(adminUserRepository, times(1)).updateLoginInfo(1L, ip);
            stpUtilMock.verify(() -> StpUtil.login(1L), times(1));
        }
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        // Given
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest, "192.168.1.1");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_PasswordError() {
        // Given
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest, "192.168.1.1");
        });

        assertEquals(ErrorCode.PASSWORD_ERROR, exception.getErrorCode());
        verify(adminUserRepository, never()).updateLoginInfo(anyLong(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 用户已禁用")
    void testLogin_UserDisabled() {
        // Given
        testUser.setEnabled(false);
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest, "192.168.1.1");
        });

        assertEquals(ErrorCode.USER_DISABLED, exception.getErrorCode());
        verify(adminUserRepository, never()).updateLoginInfo(anyLong(), anyString());
    }

    @Test
    @DisplayName("登出成功")
    void testLogout_Success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            authService.logout();

            // Then
            stpUtilMock.verify(StpUtil::logout, times(1));
        }
    }
}
