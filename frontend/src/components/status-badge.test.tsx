import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { StatusBadge } from './status-badge';

afterEach(() => {
  cleanup();
});

describe('StatusBadge', () => {
  it('exibe o rótulo PT-BR do status', () => {
    render(<StatusBadge status="READY" />);
    expect(screen.getByText('Pronto')).toBeInTheDocument();
  });

  it('aplica as classes de cor mapeadas para o status', () => {
    render(<StatusBadge status="FAILED" />);
    const badge = screen.getByText('Falhou');
    expect(badge.className).toContain('text-red-400');
  });

  it('usa estilo neutro para status desconhecido', () => {
    render(<StatusBadge status="DESCONHECIDO" />);
    const badge = screen.getByText('DESCONHECIDO');
    expect(badge.className).toContain('text-zinc-400');
  });

  it('aumenta o padding no tamanho md', () => {
    render(<StatusBadge status="READY" size="md" />);
    expect(screen.getByText('Pronto').className).toContain('px-3');
  });
});
