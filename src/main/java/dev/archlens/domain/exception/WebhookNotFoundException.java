package dev.archlens.domain.exception;

import java.util.UUID;

public class WebhookNotFoundException extends RuntimeException {

    public WebhookNotFoundException(UUID id) {
        super("Webhook not found: " + id);
    }
}
