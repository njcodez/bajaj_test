package com.example.bajajtest.runner;

import com.example.bajajtest.dto.FinalQueryRequest;
import com.example.bajajtest.dto.WebhookRequest;
import com.example.bajajtest.dto.WebhookResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StartupRunner implements CommandLineRunner {

    private static final String INITIAL_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    // The SQL that solves the "younger employees in same department" problem (from your question).
    private static final String SQL_FOR_YOUNGER_COUNT = "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
            "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
            "FROM EMPLOYEE e1 " +
            "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
            "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
            "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
            "ORDER BY e1.EMP_ID DESC;";

    // Example placeholder for alternate question (if regNo last two digits are even).
    private static final String SQL_PLACEHOLDER_ALT = "SELECT * FROM employees WHERE salary > 50000;";

    @Override
    public void run(String... args) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Prepare initial request body
            WebhookRequest webhookRequest = new WebhookRequest("John Doe", "REG12347", "john@example.com");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<WebhookRequest> requestEntity = new HttpEntity<>(webhookRequest, headers);

            // Send initial POST and get raw response as String (robust parsing)
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    INITIAL_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            System.out.println("Initial API Status: " + rawResponse.getStatusCode());
            String rawBody = rawResponse.getBody();
            System.out.println("Initial API Raw Body: " + rawBody);

            if (rawBody == null || rawBody.isBlank()) {
                System.err.println("Initial API returned empty body. Exiting.");
                return;
            }

            // Try to parse webhook and accessToken robustly from JSON (multiple possible shapes)
            String webhook = null;
            String accessToken = null;

            try {
                JsonNode root = objectMapper.readTree(rawBody);

                // Common possibilities:
                // 1) { "webhook": "...", "accessToken": "..." }
                if (root.has("webhook") && root.has("accessToken")) {
                    webhook = root.path("webhook").asText(null);
                    accessToken = root.path("accessToken").asText(null);
                }

                // 2) { "data": { "webhook": "...", "accessToken": "..." } }
                if ((webhook == null || accessToken == null) && root.has("data")) {
                    JsonNode data = root.path("data");
                    if (data.has("webhook") && data.has("accessToken")) {
                        webhook = data.path("webhook").asText(null);
                        accessToken = data.path("accessToken").asText(null);
                    }
                }

                // 3) deeper nested or different key names - try to search heuristically
                if (webhook == null || accessToken == null) {
                    // find first occurrence of fields named "webhook" and "accessToken" anywhere
                    JsonNode webhookNode = root.findValue("webhook");
                    JsonNode tokenNode = root.findValue("accessToken");
                    if (webhookNode != null) webhook = webhookNode.asText(null);
                    if (tokenNode != null) accessToken = tokenNode.asText(null);
                }

            } catch (Exception ex) {
                System.err.println("Failed to parse JSON from initial response: " + ex.getMessage());
                ex.printStackTrace();
            }

            if (webhook == null || accessToken == null) {
                System.err.println("Could not extract webhook and/or accessToken from initial response. Exiting.");
                return;
            }

            System.out.println("Resolved webhook: " + webhook);
            System.out.println("Resolved accessToken: " + (accessToken == null ? "null" : "[REDACTED]"));

            // Determine last two digits of regNo and pick SQL
            String regNo = webhookRequest.getRegNo();
            String digitsOnly = regNo.replaceAll("\\D", "");
            int lastTwo = 0;
            if (digitsOnly.length() >= 2) {
                lastTwo = Integer.parseInt(digitsOnly.substring(digitsOnly.length() - 2));
            } else if (digitsOnly.length() == 1) {
                lastTwo = Integer.parseInt(digitsOnly);
            } else {
                // fallback
                lastTwo = 0;
            }

            String finalSql;
            if (lastTwo % 2 == 1) {
                // odd -> use the provided question SQL (the "younger employees per department" problem)
                finalSql = SQL_FOR_YOUNGER_COUNT;
            } else {
                // even -> placeholder or alternate question SQL
                finalSql = SQL_PLACEHOLDER_ALT;
            }

            System.out.println("Final SQL to send: " + finalSql);

            // Build final request body
            FinalQueryRequest finalQueryRequest = new FinalQueryRequest(finalSql);

            HttpHeaders finalHeaders = new HttpHeaders();
            finalHeaders.setContentType(MediaType.APPLICATION_JSON);
            // Per your instruction, set Authorization header to the raw token (no "Bearer " prefix).
            finalHeaders.set("Authorization", accessToken);

            HttpEntity<FinalQueryRequest> finalEntity = new HttpEntity<>(finalQueryRequest, finalHeaders);

            // Send final POST to webhook
            ResponseEntity<String> finalResponse = restTemplate.exchange(
                    webhook,
                    HttpMethod.POST,
                    finalEntity,
                    String.class
            );

            System.out.println("Final POST Status: " + finalResponse.getStatusCode());
            System.out.println("Final POST Body: " + finalResponse.getBody());

        } catch (Exception e) {
            System.err.println("Unhandled error in startup runner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
