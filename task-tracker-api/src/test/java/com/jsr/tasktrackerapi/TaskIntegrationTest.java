package com.jsr.tasktrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // 4. Register -> Login -> Create task -> Get task (happy path)
    @Test
    void happyPath_registerLoginCreateGet() throws Exception {
        String token = register("alice@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"SecurePass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));

        String taskId = createTask(token, "First task", "HIGH");

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First task"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    // 5. Request without JWT to GET /tasks -> 401
    @Test
    void getTasks_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized());
    }

    // 6. USER tries to read someone else's task -> 403
    @Test
    void getForeignTask_returns403() throws Exception {
        String aliceToken = register("alice2@example.com");
        String taskId = createTask(aliceToken, "Alice private task", "LOW");

        String bobToken = register("bob2@example.com");

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // 7. PATCH status to DONE without assignee -> 400
    @Test
    void patchStatusDoneWithoutAssignee_returns400() throws Exception {
        String token = register("alice3@example.com");
        String taskId = createTask(token, "Task", "MEDIUM");

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }

    // 8. GET /tasks?status=TODO&priority=HIGH with filtering
    @Test
    void getTasks_filterByStatusAndPriority() throws Exception {
        String token = register("alice4@example.com");
        createTask(token, "High priority todo", "HIGH");

        mockMvc.perform(get("/api/v1/tasks?status=TODO&priority=HIGH")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    private String register(String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"SecurePass123\",\"name\":\"Test User\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createTask(String token, String title, String priority) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
