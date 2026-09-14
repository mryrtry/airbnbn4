package main.integration.bitrix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BitrixClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String webhookUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BitrixClient(@Value("${app.bitrix.webhook-url:}") String webhookUrl,
                        ObjectMapper objectMapper) {
        this.webhookUrl = normalizeWebhookUrl(webhookUrl);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public long createDeal(BitrixDealRequest request) throws IOException {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IOException("Bitrix24 deal title is required");
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("TITLE", request.title());
        putIfPresent(fields, "STAGE_ID", request.stageId());
        putIfPresent(fields, "COMMENTS", request.comments());
        if (request.opportunity() != null) {
            fields.put("OPPORTUNITY", request.opportunity());
            putIfPresent(fields, "CURRENCY_ID", request.currencyId());
        }

        JsonNode response = invoke("crm.deal.add.json", Map.of("FIELDS", fields));
        JsonNode result = response.get("result");
        if (result == null || !result.canConvertToLong()) {
            throw new IOException("Bitrix24 did not return a deal ID");
        }
        return result.asLong();
    }

    public void updateDeal(long dealId, BitrixDealUpdate request) throws IOException {
        if (dealId <= 0) {
            throw new IOException("Bitrix24 deal ID must be positive");
        }
        if (request == null) {
            throw new IOException("Bitrix24 deal update is required");
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "STAGE_ID", request.stageId());
        putIfPresent(fields, "COMMENTS", request.comments());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ID", dealId);
        body.put("FIELDS", fields);
        invoke("crm.deal.update.json", body);
    }

    private JsonNode invoke(String method, Object body) throws IOException {
        requireConfigured();

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl + method))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = parseResponse(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Bitrix24 HTTP error " + response.statusCode() + ": " + errorMessage(json));
            }
            if (json.has("error")) {
                throw new IOException("Bitrix24 API error: " + errorMessage(json));
            }
            return json;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Bitrix24 request was interrupted", exception);
        }
    }

    private JsonNode parseResponse(String body) throws IOException {
        JsonNode json;
        try {
            json = objectMapper.readTree(body);
        } catch (Exception exception) {
            throw new IOException("Bitrix24 returned malformed JSON", exception);
        }
        if (json == null) {
            throw new IOException("Bitrix24 returned an empty response");
        }
        return json;
    }

    private String errorMessage(JsonNode response) {
        if (response.hasNonNull("error_description")) {
            return response.get("error_description").asText();
        }
        if (response.hasNonNull("error")) {
            return response.get("error").asText();
        }
        return "unknown error";
    }

    private void requireConfigured() throws IOException {
        if (webhookUrl.isBlank()) {
            throw new IOException("Bitrix24 webhook URL is not configured");
        }
    }

    private void putIfPresent(Map<String, Object> target, String name, String value) {
        if (value != null && !value.isBlank()) {
            target.put(name, value);
        }
    }

    private String normalizeWebhookUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            throw new IllegalArgumentException("Bitrix24 webhook URL must use HTTP or HTTPS");
        }
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
