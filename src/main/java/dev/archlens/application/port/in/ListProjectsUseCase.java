package dev.archlens.application.port.in;

import java.util.List;

import dev.archlens.domain.model.Project;

public interface ListProjectsUseCase {

    List<Project> listAll();
}
