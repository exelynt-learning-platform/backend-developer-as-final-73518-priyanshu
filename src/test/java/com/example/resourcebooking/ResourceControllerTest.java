package com.example.resourcebooking;

import com.example.resourcebooking.dto.LoginRequest;
import com.example.resourcebooking.dto.ResourceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        userToken = login("user", "user123");
    }

    @Test
    void adminCanCreateResource() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Conference Room A", "Large meeting room", "room")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Conference Room A"));
    }

    @Test
    void userCannotCreateResource() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Vehicle", "Company car", "vehicle")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanReadResources() throws Exception {
        createResource(adminToken, "Room B", "Small room", "room");

        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminCanUpdateResource() throws Exception {
        Long id = createResource(adminToken, "Old Name", "desc", "room");

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("New Name", "updated desc", "room")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void adminCanDeleteResource() throws Exception {
        Long id = createResource(adminToken, "To Delete", "desc", "equipment");

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createResourceWithMissingName_returns400() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"test\",\"type\":\"room\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotAccessResources() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resourcesArePaginated() throws Exception {
        for (int i = 0; i < 5; i++) {
            createResource(adminToken, "Room " + i, "desc", "room");
        }

        mockMvc.perform(get("/api/resources?page=0&size=2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    private String login(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createResource(String token, String name, String description, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson(name, description, type)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String resourceJson(String name, String description, String type) {
        return String.format("{\"name\":\"%s\",\"description\":\"%s\",\"type\":\"%s\"}", name, description, type);
    }
}
