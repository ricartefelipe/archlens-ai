'use client';

import { useSyncExternalStore } from 'react';

const emptySubscribe = () => () => {};

/**
 * Lê um valor disponível apenas no cliente (ex.: localStorage via helpers de
 * auth) de forma segura para hidratação: o servidor renderiza o fallback e o
 * cliente assume o valor real no primeiro render pós-hidratação, sem
 * setState dentro de useEffect.
 *
 * O snapshot deve retornar valores primitivos/estáveis (string, boolean),
 * comparados por Object.is.
 */
export function useClientValue<T>(getSnapshot: () => T, serverFallback: T): T {
  return useSyncExternalStore(emptySubscribe, getSnapshot, () => serverFallback);
}
