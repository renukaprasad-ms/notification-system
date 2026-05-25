package com.renuka.notification_backend;

import com.renuka.notification_backend.user.entity.RoleName;
import com.renuka.notification_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupCreatesUserAndIssuesAuthCookies() throws Exception {
        String email = uniqueEmail("signup");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(json())
                        .content(toJson(Map.of(
                                "email", email,
                                "password", "Password@123",
                                "fullName", "Signup User"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.fullName").value("Signup User"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("access_token="))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("refresh_token="))));

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        mockMvc.perform(authenticated(get("/api/auth/me"), savedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    void loginWithPasswordIssuesFreshAuthCookies() throws Exception {
        String email = uniqueEmail("login");
        createUserWithRoles(email, "Password@123", "Login User", RoleName.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(json())
                        .content(toJson(Map.of(
                                "email", email,
                                "password", "Password@123",
                                "loginType", "EMAIL_PASSWORD"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("access_token="))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("refresh_token="))));
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        String email = uniqueEmail("invalid-login");
        createUserWithRoles(email, "Password@123", "Wrong Password User", RoleName.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(json())
                        .content(toJson(Map.of(
                                "email", email,
                                "password", "Wrong@123",
                                "loginType", "EMAIL_PASSWORD"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.error_message").value("Invalid email or password"));
    }
}
