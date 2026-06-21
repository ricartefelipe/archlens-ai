export function parseApiError(err: unknown): string {
  if (!(err instanceof Error)) {
    return 'Erro inesperado. Tente novamente.';
  }

  const match = err.message.match(/^API (\d+): ([\s\S]*)$/);
  if (!match) {
    return err.message;
  }

  const payload = match[2]?.trim();
  if (!payload) {
    return `Erro na API (${match[1]}).`;
  }

  try {
    const body = JSON.parse(payload) as { message?: string };
    if (body.message) {
      return body.message;
    }
  } catch {
    return payload;
  }

  return `Erro na API (${match[1]}).`;
}
