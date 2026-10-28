/**
 * Prefixos servidos pela API (backend Quarkus), não por páginas do Next.
 *
 * A UI usa a mesma origem para a API (ver `api-base.ts`), então estes caminhos
 * precisam ser encaminhados ao backend (`next.config.ts`) e ficar fora da
 * proteção de rotas de página (`proxy.ts`): a autorização deles é feita pelo
 * backend com JWT, não pelo cookie de tenant.
 */
export const API_PREFIXES = ['/v1', '/public', '/q'] as const;

export function isApiPath(pathname: string): boolean {
  return API_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  );
}
