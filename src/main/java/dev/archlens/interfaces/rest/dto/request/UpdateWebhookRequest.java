package dev.archlens.interfaces.rest.dto.request;

import java.util.List;

public record UpdateWebhookRequest(
        String url,
        List<String> events,
        Boolean enabled) {
}
