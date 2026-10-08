package com.example.tickets;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP, real H2, real async executor, and the mock LLM provider. */
@SpringBootTest(properties = "llm.retry.backoff-ms=10")
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Autowired MockMvc mvc;

    private static String ticketJson(String description) {
        return """
                {"customerId":"CUST-101","subject":"Unable to import a Word document",
                 "description":"%s","priority":"HIGH","product":"Import"}""".formatted(description);
    }

    private String createTicket(String description) throws Exception {
        MvcResult res = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(description)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.ticketId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.analysis").doesNotExist())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.ticketId");
    }

    @Test
    void createsTicketThenAnalysisCompletesAsynchronously() throws Exception {
        String id = createTicket("The import remains at 80% and eventually fails.");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                mvc.perform(get("/api/tickets/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.analysis.category").value("IMPORT_FAILURE"))
                        .andExpect(jsonPath("$.analysis.recommendedTeam").value("IMPORT_ENGINEERING"))
                        .andExpect(jsonPath("$.analysis.confidence").isNumber()));
    }

    @Test
    void llmTimeoutEndsAsFailedWithoutAnalysis() throws Exception {
        String id = createTicket("Import hangs [mock-timeout]");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                mvc.perform(get("/api/tickets/" + id))
                        .andExpect(jsonPath("$.status").value("FAILED"))
                        .andExpect(jsonPath("$.failureReason").isNotEmpty())
                        .andExpect(jsonPath("$.analysis").doesNotExist()));
    }

    @Test
    void invalidLlmOutputEndsAsFailed() throws Exception {
        String id = createTicket("Something odd [mock-invalid]");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                mvc.perform(get("/api/tickets/" + id))
                        .andExpect(jsonPath("$.status").value("FAILED"))
                        .andExpect(jsonPath("$.analysis").doesNotExist()));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(4));
    }

    @Test
    void rejectsInvalidPriorityAndMalformedJson() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("x").replace("HIGH", "URGENT")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTicketReturns404() throws Exception {
        mvc.perform(get("/api/tickets/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: does-not-exist"));
    }
}
