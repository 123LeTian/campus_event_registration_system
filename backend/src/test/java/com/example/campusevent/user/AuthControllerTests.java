package com.example.campusevent.user;

import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.user.entity.User;
import com.example.campusevent.user.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void registerCreatesStudentWithoutPasswordInResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"newStudent","password":"Student123456","name":"新学生"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("newStudent"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"studentA","password":"Student123456","name":"重复学生"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void registerStoresBCryptPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"bcryptUser","password":"Student123456","name":"加密用户"}
                                """))
                .andExpect(status().isOk());

        User user = userMapper.findByUsername("bcryptUser");
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(user.getPassword()).isNotEqualTo("Student123456");
        assertThat(passwordEncoder.matches("Student123456", user.getPassword())).isTrue();
    }

    @Test
    void registerIgnoresInjectedAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"roleHacker","password":"Password123","name":"测试用户","role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));

        assertThat(userMapper.findByUsername("roleHacker").getRole()).isEqualTo("STUDENT");
    }

    @Test
    void loginReturnsJwtAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"organizer1","password":"Org123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("ORGANIZER"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"organizer1","password":"WrongPassword"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"disabledUser","password":"Stu123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    void meAcceptsValidToken() throws Exception {
        String token = jwtTokenProvider.createToken(userMapper.findByUsername("studentA"));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("studentA"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void illegalTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer bad.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        User user = userMapper.findByUsername("studentA");
        SecretKey secretKey = Keys.hmacShaKeyFor("test-development-secret-change-me-test-development-secret-change-me"
                .getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(new Date(now.getTime() - 7_200_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(secretKey)
                .compact();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void rolesAreParsedFromJwt() {
        assertThat(jwtTokenProvider.getAuthentication(jwtTokenProvider.createToken(userMapper.findByUsername("studentA")))
                .getAuthorities()).extracting("authority").contains("ROLE_STUDENT");
        assertThat(jwtTokenProvider.getAuthentication(jwtTokenProvider.createToken(userMapper.findByUsername("organizer1")))
                .getAuthorities()).extracting("authority").contains("ROLE_ORGANIZER");
        assertThat(jwtTokenProvider.getAuthentication(jwtTokenProvider.createToken(userMapper.findByUsername("admin")))
                .getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void roleProtectedEndpointDistinguishes401And403() throws Exception {
        String studentToken = jwtTokenProvider.createToken(userMapper.findByUsername("studentA"));
        String adminToken = jwtTokenProvider.createToken(userMapper.findByUsername("admin"));

        mockMvc.perform(get("/api/security/admin-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(get("/api/security/admin-check").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/security/admin-check").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
