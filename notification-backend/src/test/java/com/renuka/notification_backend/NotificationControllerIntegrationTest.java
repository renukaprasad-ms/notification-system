package com.renuka.notification_backend;

import com.renuka.notification_backend.user.entity.RoleName;
import com.renuka.notification_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sendSelectedNotificationSupportsInboxAndUnreadFlow() throws Exception {
        User admin = createUserWithRoles(uniqueEmail("admin"), "Admin@12345", "Admin User", RoleName.ADMIN);
        User recipient = createUserWithRoles(uniqueEmail("recipient"), "Password@123", "Recipient User", RoleName.USER);

        mockMvc.perform(authenticated(post("/api/notifications/send-selected"), admin)
                        .contentType(json())
                        .content(toJson(Map.of(
                                "title", "Policy update",
                                "message", "Your access has been updated.",
                                "type", "SYSTEM",
                                "priority", "HIGH",
                                "requestId", "req-" + recipient.getId(),
                                "recipientUserIds", List.of(recipient.getId())
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.recipientCount").value(1));

        mockMvc.perform(authenticated(get("/api/notifications/me/unread-count"), recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        MvcResult listResult = mockMvc.perform(authenticated(get("/api/notifications/me"), recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Policy update"))
                .andExpect(jsonPath("$.data.items[0].readAt").doesNotExist())
                .andReturn();

        JsonNode listPayload = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String recipientId = listPayload.path("data").path("items").get(0).path("recipientId").asText();

        mockMvc.perform(authenticated(patch("/api/notifications/{recipientId}/read", recipientId), recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.title").value("Policy update"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(authenticated(get("/api/notifications/me/unread-count"), recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(authenticated(get("/api/notifications/me/stats"), recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotifications").value(1))
                .andExpect(jsonPath("$.data.unreadNotifications").value(0))
                .andExpect(jsonPath("$.data.readNotifications").value(1));
    }
}
