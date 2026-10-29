package dev.archlens.infrastructure.persistence.rls;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

/**
 * Aplica o tenant do request como contexto de RLS <em>na transação em que as
 * queries do método vão rodar</em>.
 *
 * A versão anterior chamava um método {@code @Transactional} que abria
 * transação própria, executava o {@code set_config(..., true)} — local à
 * transação — e commitava antes de o método real começar: o setting evaporava
 * e nenhuma query enxergava o tenant. Isso passou despercebido enquanto a
 * política de RLS tinha fallback permissivo; com a política restritiva, toda
 * leitura fora de transação voltava vazia.
 *
 * Métodos já transacionais (o interceptor de {@code @Transactional} roda antes
 * deste, por prioridade) apenas ganham o setting dentro da transação existente;
 * os demais passam a rodar dentro de uma transação aberta aqui.
 */
@TenantScopedRls
@Interceptor
@Priority(Interceptor.Priority.APPLICATION - 10)
public class TenantRlsInterceptor {

    private final TenantRlsService tenantRlsService;
    private final TransactionManager transactionManager;

    @Inject
    public TenantRlsInterceptor(TenantRlsService tenantRlsService,
                                TransactionManager transactionManager) {
        this.tenantRlsService = tenantRlsService;
        this.transactionManager = transactionManager;
    }

    @AroundInvoke
    public Object applyTenant(InvocationContext context) throws Exception {
        if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            tenantRlsService.applyCurrentTenant();
            return context.proceed();
        }

        try {
            return QuarkusTransaction.requiringNew().call(() -> {
                tenantRlsService.applyCurrentTenant();
                try {
                    return context.proceed();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CheckedWrapper(e);
                }
            });
        } catch (CheckedWrapper wrapper) {
            throw (Exception) wrapper.getCause();
        }
    }

    /** Transporta exceção checked através do callable sem mudar seu tipo. */
    private static final class CheckedWrapper extends RuntimeException {
        private CheckedWrapper(Exception cause) {
            super(cause);
        }
    }
}
