import { afterEach, describe, expect, it } from 'vitest';
import { getTenantId, isAuthenticated, login, logout, setTenantId } from './auth';

afterEach(() => {
  localStorage.clear();
});

describe('auth', () => {
  it('retorna "default" quando não há tenant salvo', () => {
    expect(getTenantId()).toBe('default');
    expect(isAuthenticated()).toBe(false);
  });

  it('persiste o tenant e o token em login e o expõe em getTenantId', () => {
    login('tenant-1', 'access-token');
    expect(getTenantId()).toBe('tenant-1');
    expect(isAuthenticated()).toBe(true);
  });

  it('setTenantId atualiza o valor armazenado', () => {
    setTenantId('tenant-2');
    expect(getTenantId()).toBe('tenant-2');
  });

  it('logout remove o tenant e volta ao estado não autenticado', () => {
    login('tenant-1', 'access-token');
    logout();
    expect(isAuthenticated()).toBe(false);
    expect(getTenantId()).toBe('default');
  });
});
