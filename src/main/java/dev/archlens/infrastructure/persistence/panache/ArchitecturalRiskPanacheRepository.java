package dev.archlens.infrastructure.persistence.panache;

import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.ArchitecturalRiskEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArchitecturalRiskPanacheRepository implements PanacheRepositoryBase<ArchitecturalRiskEntity, UUID> {
}
