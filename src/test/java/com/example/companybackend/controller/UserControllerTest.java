package com.example.companybackend.controller;

import com.example.companybackend.entity.User;
import com.example.companybackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, UserControllerTest.TestSecurityConfig.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User testUser;
    private List<User> testUsers;

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .build();
        }
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmployeeId("EMP001");
        testUser.setFullName("田中太郎");
        testUser.setEmail("tanaka@example.com");
        testUser.setRole("admin");
        testUser.setDepartmentId(1);
        testUser.setPositionId(1);
        testUser.setLocationType("office");
        testUser.setHireDate(LocalDate.of(2020, 4, 1));
        testUser.setPhone("090-1234-5678");
        testUser.setPasswordHash("password");
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());
        testUser.setIsActive(true);

        testUsers = Arrays.asList(testUser);
    }

    /**
     * ユーザー一覧取得のテスト
     * 管理者権限を持つユーザーがユーザー一覧を取得できることを検証する
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUsers_Success() throws Exception {
        Map<String, Object> usersData = new HashMap<>();
        usersData.put("users", testUsers);
        usersData.put("totalCount", 1L);
        usersData.put("currentPage", 0);
        usersData.put("totalPages", 1);

        when(userService.getUsers(0, 10, null, null, null, null, null))
                .thenReturn(usersData);

        mockMvc.perform(get("/api/users/list")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.length()").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(userService, times(1)).getUsers(0, 10, null, null, null, null, null);
    }

    /**
     * ユーザーIDによるユーザー情報取得のテスト
     * 管理者権限を持つユーザーが特定のユーザー情報を取得できることを検証する
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUserById_Success() throws Exception {
        when(userService.findById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(userService, times(1)).findById(1L);
    }

    /**
     * 新規ユーザー作成のテスト
     * 管理者権限を持つユーザーが新規ユーザーを作成できることを検証する
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_Success() throws Exception {
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        String userJson = "{"
                + "\"username\":\"newuser\","
                + "\"fullName\":\"新規ユーザー\","
                + "\"email\":\"new@example.com\","
                + "\"passwordHash\":\"password\","
                + "\"locationType\":\"office\""
                + "}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(userService, times(1)).createUser(any(User.class));
    }

    /**
     * ユーザー情報更新のテスト
     * 管理者権限を持つユーザーが既存のユーザー情報を更新できることを検証する
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUser_Success() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setFullName("田中太郎（更新）");
        updatedUser.setEmail("tanaka.updated@example.com");
        updatedUser.setPhone("080-9999-8888");
        updatedUser.setUpdatedAt(OffsetDateTime.now());

        when(userService.findById(1L)).thenReturn(testUser);
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        String updateJson = "{"
                + "\"fullName\":\"田中太郎（更新）\","
                + "\"email\":\"tanaka.updated@example.com\","
                + "\"phone\":\"080-9999-8888\""
                + "}";

        mockMvc.perform(put("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("田中太郎（更新）"));

        verify(userService, times(1)).findById(1L);
        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    /**
     * ユーザー削除のテスト
     * 管理者権限を持つユーザーがユーザーを削除できることを検証する
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_Success() throws Exception {
        when(userService.findById(1L)).thenReturn(testUser);
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ユーザーが正常に削除されました"));

        verify(userService, times(1)).findById(1L);
        verify(userService, times(1)).deleteUser(1L);
    }

    /**
     * 現在のユーザー情報取得のテスト
     * 認証されたユーザーが自分の情報を取得できることを検証する
     */
    @Test
    @WithMockUser
    void testGetCurrentUser_Success() throws Exception {
        when(userService.findByUsername("user")).thenReturn(testUser);

        mockMvc.perform(get("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(userService, times(1)).findByUsername("user");
    }

    /**
     * パスワード変更（成功ケース）のテスト
     * ユーザーが正しい現在のパスワードを使用してパスワードを変更できることを検証する
     */
    @Test
    @WithMockUser
    void testChangePassword_Success() throws Exception {
        when(userService.changePassword(eq("user"), eq("oldPassword"), eq("newPassword"))).thenReturn(true);

        String passwordJson = "{"
                + "\"oldPassword\":\"oldPassword\","
                + "\"newPassword\":\"newPassword\""
                + "}";

        mockMvc.perform(post("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("パスワードが正常に変更されました"));

        verify(userService, times(1)).changePassword(eq("user"), eq("oldPassword"), eq("newPassword"));
    }

    /**
     * パスワード変更（失敗ケース：現在のパスワードが間違っている）のテスト
     * ユーザーが間違った現在のパスワードを使用した場合にパスワード変更が失敗することを検証する
     */
    @Test
    @WithMockUser
    void testChangePassword_WrongOldPassword() throws Exception {
        when(userService.changePassword(eq("user"), eq("wrongPassword"), eq("newPassword"))).thenReturn(false);

        String passwordJson = "{"
                + "\"oldPassword\":\"wrongPassword\","
                + "\"newPassword\":\"newPassword\""
                + "}";

        mockMvc.perform(post("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("現在のパスワードが正しくありません"));

        verify(userService, times(1)).changePassword(eq("user"), eq("wrongPassword"), eq("newPassword"));
    }
}