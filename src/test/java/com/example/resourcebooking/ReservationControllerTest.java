package com.example.resourcebooking;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private Long resourceId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        userToken = login("user", "user123");
        resourceId = createResource(adminToken, "Meeting Room", "desc", "room");
    }

    @Test
    void userCanCreateReservation() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, "2027-01-10T09:00:00", "2027-01-10T10:00:00", "100.00", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void userCanViewOwnReservation() throws Exception {
        Long id = createReservation(userToken, resourceId, "2027-02-10T09:00:00", "2027-02-10T10:00:00", "50.00");

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void userCannotViewAnotherUsersReservation() throws Exception {
        Long id = createReservation(adminToken, resourceId, "2027-03-10T09:00:00", "2027-03-10T10:00:00", "75.00");

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAllReservations() throws Exception {
        createReservation(userToken, resourceId, "2027-04-10T09:00:00", "2027-04-10T10:00:00", "100.00");
        createReservation(adminToken, resourceId, "2027-04-11T09:00:00", "2027-04-11T10:00:00", "200.00");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void userOnlySeesOwnReservations() throws Exception {
        createReservation(userToken, resourceId, "2027-05-10T09:00:00", "2027-05-10T10:00:00", "100.00");
        createReservation(adminToken, resourceId, "2027-05-11T09:00:00", "2027-05-11T10:00:00", "200.00");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("user"));
    }

    @Test
    void filterByStatus() throws Exception {
        createReservationWithStatus(adminToken, resourceId, "2027-06-10T09:00:00", "2027-06-10T10:00:00", "100.00", "CONFIRMED");
        createReservationWithStatus(adminToken, resourceId, "2027-06-11T09:00:00", "2027-06-11T10:00:00", "200.00", "PENDING");

        mockMvc.perform(get("/api/reservations?status=CONFIRMED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void filterByMinPrice() throws Exception {
        createReservation(adminToken, resourceId, "2027-07-10T09:00:00", "2027-07-10T10:00:00", "50.00");
        createReservation(adminToken, resourceId, "2027-07-11T09:00:00", "2027-07-11T10:00:00", "150.00");

        mockMvc.perform(get("/api/reservations?minPrice=100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(150.00));
    }

    @Test
    void filterByMaxPrice() throws Exception {
        createReservation(adminToken, resourceId, "2027-08-10T09:00:00", "2027-08-10T10:00:00", "50.00");
        createReservation(adminToken, resourceId, "2027-08-11T09:00:00", "2027-08-11T10:00:00", "150.00");

        mockMvc.perform(get("/api/reservations?maxPrice=100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(50.00));
    }

    @Test
    void paginationWorks() throws Exception {
        for (int i = 0; i < 5; i++) {
            int hour = 9 + i;
            createReservation(adminToken, resourceId,
                    "2027-09-10T0" + hour + ":00:00",
                    "2027-09-10T" + (hour + 1) + ":00:00",
                    String.valueOf(100 + i * 10));
        }

        mockMvc.perform(get("/api/reservations?page=0&size=2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void sortingByPriceWorks() throws Exception {
        createReservation(adminToken, resourceId, "2027-10-10T09:00:00", "2027-10-10T10:00:00", "300.00");
        createReservation(adminToken, resourceId, "2027-10-11T09:00:00", "2027-10-11T10:00:00", "100.00");
        createReservation(adminToken, resourceId, "2027-10-12T09:00:00", "2027-10-12T10:00:00", "200.00");

        MvcResult result = mockMvc.perform(get("/api/reservations?sort=price,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        assertThat(content.get(0).get("price").asDouble()).isLessThan(content.get(1).get("price").asDouble());
        assertThat(content.get(1).get("price").asDouble()).isLessThan(content.get(2).get("price").asDouble());
    }

    @Test
    void invalidStartEndTime_returns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, "2027-01-10T11:00:00", "2027-01-10T09:00:00", "100.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePrice_returns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, "2027-01-10T09:00:00", "2027-01-10T10:00:00", "-10.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanDeleteAnyReservation() throws Exception {
        Long id = createReservation(userToken, resourceId, "2027-11-10T09:00:00", "2027-11-10T10:00:00", "100.00");

        mockMvc.perform(delete("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotDeleteAnotherUsersReservation() throws Exception {
        Long id = createReservation(adminToken, resourceId, "2027-12-10T09:00:00", "2027-12-10T10:00:00", "100.00");

        mockMvc.perform(delete("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String login(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createResource(String token, String name, String desc, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"name\":\"%s\",\"description\":\"%s\",\"type\":\"%s\"}", name, desc, type)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createReservation(String token, Long resId, String start, String end, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resId, start, end, price, null)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createReservationWithStatus(String token, Long resId, String start, String end,
                                             String price, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resId, start, end, price, status)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String reservationJson(Long resId, String start, String end, String price, String status) {
        String statusPart = status != null ? ",\"status\":\"" + status + "\"" : "";
        return String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"price\":%s%s}",
                resId, start, end, price, statusPart
        );
    }
}
