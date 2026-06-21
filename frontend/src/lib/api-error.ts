export function parseApiError(err: unknown): string {
  if (!(err instanceof Error)) {
    return 'Erro inesperado. Tente novamente.';
  }

  const match = err.message.match(/^API (\d+): ([\s\S]*)$/);
  if (!match) {
    return err.message;
  }

  const status = match[1];
  const payload = match[2]?.trim();

  if (status === '413') {
    return 'Arquivo muito grande. Envie um ZIP de até 200 MB (sem node_modules, dist ou .git).';
  }

  if (!payload) {
    return `Erro na API (${status}).`;
  }

  if (payload.includes('Request Entity Too Large')) {
    return 'Arquivo muito grande. Envie um ZIP de até 200 MB (sem node_modules, dist ou .git).';
  }

  try {
    const body = JSON.parse(payload) as { message?: string };
    if (body.message) {
      return body.message;
    }
  } catch {
    return payload;
  }

  return `Erro na API (${status}).`;
}
