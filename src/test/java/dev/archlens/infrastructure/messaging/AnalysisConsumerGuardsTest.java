package dev.archlens.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.archlens.domain.model.AnalysisStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guardas do consumidor: revalidação de tenant no job e idempotência diante de
 * reentrega da mensagem (worker que morre antes do ACK).
 */
class AnalysisConsumerGuardsTest {

    @Test
    @DisplayName("tenant do evento precisa ser exatamente o da análise")
    void refusesEventFromAnotherTenant() {
        assertTrue(AnalysisConsumer.tenantMatches("tenant-1", "tenant-1"));
        assertFalse(AnalysisConsumer.tenantMatches("tenant-1", "tenant-2"));
        assertFalse(AnalysisConsumer.tenantMatches("tenant-1", null));
        assertFalse(AnalysisConsumer.tenantMatches(null, "tenant-1"));
    }

    @Test
    @DisplayName("mensagem reentregue de análise concluída ou cancelada é ignorada")
    void ignoresRedeliveryOfCompletedAnalysis() {
        assertTrue(AnalysisConsumer.isAlreadySettled(AnalysisStatus.COMPLETED));
        // Cancelada também: reprocessar reviveria algo que o usuário parou.
        assertTrue(AnalysisConsumer.isAlreadySettled(AnalysisStatus.CANCELLED));
    }

    @Test
    @DisplayName("estados não terminais permitem retomada")
    void allowsResumeForNonSettledStates() {
        assertFalse(AnalysisConsumer.isAlreadySettled(AnalysisStatus.PENDING));
        assertFalse(AnalysisConsumer.isAlreadySettled(AnalysisStatus.PROCESSING));
        // FAILED fica de fora de propósito: permite retentativa após falha
        // transitória (worker indisponível, por exemplo).
        assertFalse(AnalysisConsumer.isAlreadySettled(AnalysisStatus.FAILED));
    }
}
