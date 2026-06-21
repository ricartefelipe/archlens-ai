package dev.archlens.interfaces.rest.mapper;

import java.util.Arrays;
import java.util.List;

import dev.archlens.application.service.AdminTenantService.TenantAdminView;
import dev.archlens.application.service.QuotaService.UsageSnapshot;
import dev.archlens.domain.model.ApiKeyRecord;
import dev.archlens.domain.model.OrgInvite;
import dev.archlens.domain.model.OrgMember;
import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.interfaces.rest.dto.response.AdminTenantResponse;
import dev.archlens.interfaces.rest.dto.response.ApiKeyCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.ApiKeyResponse;
import dev.archlens.interfaces.rest.dto.response.OrgInviteCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.OrgInviteResponse;
import dev.archlens.interfaces.rest.dto.response.OrgMemberResponse;
import dev.archlens.interfaces.rest.dto.response.WebhookCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.WebhookResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlatformDtoMapper {

    public OrgMemberResponse toMemberResponse(OrgMember member) {
        return new OrgMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }

    public List<OrgMemberResponse> toMemberResponseList(List<OrgMember> members) {
        return members.stream().map(this::toMemberResponse).toList();
    }

    public OrgInviteResponse toInviteResponse(OrgInvite invite) {
        return new OrgInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole().name(),
                invite.getExpiresAt(),
                invite.getCreatedAt());
    }

    public List<OrgInviteResponse> toInviteResponseList(List<OrgInvite> invites) {
        return invites.stream().map(this::toInviteResponse).toList();
    }

    public OrgInviteCreatedResponse toInviteCreatedResponse(OrgInvite invite, String token) {
        return new OrgInviteCreatedResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole().name(),
                token,
                invite.getExpiresAt());
    }

    public ApiKeyResponse toApiKeyResponse(ApiKeyRecord record) {
        return new ApiKeyResponse(
                record.getId(),
                record.getName(),
                record.getKeyPrefix(),
                record.getScopes(),
                record.getCreatedAt(),
                record.getRevokedAt(),
                record.getLastUsedAt());
    }

    public List<ApiKeyResponse> toApiKeyResponseList(List<ApiKeyRecord> records) {
        return records.stream().map(this::toApiKeyResponse).toList();
    }

    public ApiKeyCreatedResponse toApiKeyCreatedResponse(ApiKeyRecord record, String plainKey) {
        return new ApiKeyCreatedResponse(
                record.getId(),
                record.getName(),
                plainKey,
                record.getScopes(),
                record.getCreatedAt());
    }

    public WebhookResponse toWebhookResponse(TenantWebhook webhook) {
        return new WebhookResponse(
                webhook.getId(),
                webhook.getUrl(),
                parseEvents(webhook.getEvents()),
                webhook.isEnabled(),
                webhook.getCreatedAt(),
                webhook.getUpdatedAt());
    }

    public List<WebhookResponse> toWebhookResponseList(List<TenantWebhook> webhooks) {
        return webhooks.stream().map(this::toWebhookResponse).toList();
    }

    public WebhookCreatedResponse toWebhookCreatedResponse(TenantWebhook webhook, String secret) {
        return new WebhookCreatedResponse(
                webhook.getId(),
                webhook.getUrl(),
                secret,
                webhook.getEvents(),
                webhook.isEnabled(),
                webhook.getCreatedAt());
    }

    public AdminTenantResponse toAdminTenantResponse(TenantAdminView view) {
        UsageSnapshot snapshot = view.usage();
        return new AdminTenantResponse(
                snapshot.tenantId(),
                snapshot.plan().name(),
                snapshot.plan().displayName(),
                snapshot.status().name(),
                snapshot.projectsUsed(),
                snapshot.projectsLimit(),
                snapshot.analysesUsed(),
                snapshot.analysesLimit(),
                snapshot.uploadBytesUsed() / (1024 * 1024),
                snapshot.uploadMbLimit(),
                snapshot.usagePeriodStart(),
                view.notes());
    }

    private static List<String> parseEvents(String events) {
        if (events == null || events.isBlank()) {
            return List.of();
        }
        return Arrays.asList(events.split(","));
    }
}
