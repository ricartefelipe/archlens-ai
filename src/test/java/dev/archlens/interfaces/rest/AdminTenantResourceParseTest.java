package dev.archlens.interfaces.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.TenantAccountStatus;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Conversão dos enums recebidos pela API de administração. {@code Enum.valueOf}
 * lançava {@code IllegalArgumentException}, que virava 500: um valor errado
 * enviado pelo cliente era reportado como falha do servidor.
 */
class AdminTenantResourceParseTest {

    @Test
    @DisplayName("aceita o valor exato do enum")
    void acceptsExactValue() {
        assertEquals(CommercialPlan.DIAGNOSTICO,
                AdminTenantResource.parseEnum(CommercialPlan.class, "DIAGNOSTICO", "plan"));
        assertEquals(TenantAccountStatus.ACTIVE,
                AdminTenantResource.parseEnum(TenantAccountStatus.class, "ACTIVE", "status"));
    }

    @Test
    @DisplayName("tolera diferença de caixa e espaços em volta")
    void toleratesCaseAndWhitespace() {
        assertEquals(CommercialPlan.PORTFOLIO,
                AdminTenantResource.parseEnum(CommercialPlan.class, " portfolio ", "plan"));
    }

    @Test
    @DisplayName("valor inválido vira 400 com a lista de aceitos")
    void invalidValueBecomesBadRequest() {
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> AdminTenantResource.parseEnum(CommercialPlan.class, "AUDITORIA", "plan"));

        assertEquals(400, error.getResponse().getStatus());
        assertTrue(error.getMessage().contains("plan"), error.getMessage());
        // A mensagem precisa dizer o que é aceito, para o cliente corrigir.
        assertTrue(error.getMessage().contains("PILOT"), error.getMessage());
        assertTrue(error.getMessage().contains("DIAGNOSTICO"), error.getMessage());
    }

    @Test
    @DisplayName("nulo e vazio também viram 400, não erro de execução")
    void nullAndBlankBecomeBadRequest() {
        assertThrows(BadRequestException.class,
                () -> AdminTenantResource.parseEnum(CommercialPlan.class, null, "plan"));
        assertThrows(BadRequestException.class,
                () -> AdminTenantResource.parseEnum(TenantAccountStatus.class, "  ", "status"));
    }
}
