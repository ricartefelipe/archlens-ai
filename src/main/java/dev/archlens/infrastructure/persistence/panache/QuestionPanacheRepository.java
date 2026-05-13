package dev.archlens.infrastructure.persistence.panache;

import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.QuestionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuestionPanacheRepository implements PanacheRepositoryBase<QuestionEntity, UUID> {
}
