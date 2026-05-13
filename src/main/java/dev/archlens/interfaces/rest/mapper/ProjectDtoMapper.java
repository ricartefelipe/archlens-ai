package dev.archlens.interfaces.rest.mapper;

import java.util.List;

import dev.archlens.domain.model.Project;
import dev.archlens.interfaces.rest.dto.response.ProjectResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectDtoMapper {

    public ProjectResponse toResponse(Project domain) {
        return new ProjectResponse(
                domain.getId(),
                domain.getTenantId(),
                domain.getName(),
                domain.getDescription(),
                domain.getStatus() != null ? domain.getStatus().name() : null,
                domain.getFileCount(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }

    public List<ProjectResponse> toResponseList(List<Project> domains) {
        return domains.stream()
                .map(this::toResponse)
                .toList();
    }
}
