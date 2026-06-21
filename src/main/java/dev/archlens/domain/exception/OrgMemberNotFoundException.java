package dev.archlens.domain.exception;

import java.util.UUID;

public class OrgMemberNotFoundException extends RuntimeException {

    public OrgMemberNotFoundException(UUID id) {
        super("Org member not found: " + id);
    }
}
