/** Base da API: mesma origem no browser (evita http vs https no piloto). */
export function getApiBase(): string {
  if (typeof window !== 'undefined') {
    return window.location.origin;
  }
  return process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
}
