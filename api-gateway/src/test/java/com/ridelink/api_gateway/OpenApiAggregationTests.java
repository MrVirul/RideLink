package com.ridelink.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The gateway is the single place the aggregated Swagger UI is meant to work, so assert the
 * dropdown really lists every service spec. Eureka is switched off because nothing here
 * needs a running registry.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class OpenApiAggregationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUiConfigListsEveryService() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/swagger-config")).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "/v3/api-docs/swagger-config should be served");

        String config = result.getResponse().getContentAsString();
        for (String service : List.of("account-service", "driver-service", "ride-service", "fare-service")) {
            assertTrue(config.contains("/" + service + "/v3/api-docs"),
                    "aggregated UI should offer the " + service + " spec");
        }
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertTrue(status == 200 || (status >= 300 && status < 400),
                "/swagger-ui.html should render or redirect to the UI, got " + status);
    }
}
