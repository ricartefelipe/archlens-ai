import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createProject, listProjects, uploadProjectZip } from './api';

const API_BASE = 'http://localhost:8080';

function jsonResponse(data: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: 'OK',
    json: async () => data,
    text: async () => JSON.stringify(data),
  };
}

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('apiFetch (via createProject)', () => {
  it('envia POST com headers de tenant, correlação e content-type JSON', async () => {
    const project = { id: 'p1', name: 'checkout' };
    fetchMock.mockResolvedValueOnce(jsonResponse(project, 201));

    const result = await createProject('tenant-1', 'checkout', 'desc');

    expect(result).toEqual(project);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe(`${API_BASE}/v1/projects`);
    expect(init.method).toBe('POST');
    expect(init.headers['X-Tenant-Id']).toBe('tenant-1');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.headers['X-Correlation-Id']).toBeTruthy();
    expect(JSON.parse(init.body)).toEqual({ name: 'checkout', description: 'desc' });
  });
});

describe('listProjects', () => {
  it('faz GET e retorna a lista', async () => {
    const projects = [{ id: 'p1' }, { id: 'p2' }];
    fetchMock.mockResolvedValueOnce(jsonResponse(projects));

    const result = await listProjects('tenant-1');

    expect(result).toEqual(projects);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe(`${API_BASE}/v1/projects`);
  });

  it('lança erro com status quando a resposta não é ok', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: async () => 'boom',
      json: async () => ({}),
    });

    await expect(listProjects('tenant-1')).rejects.toThrow('API 500');
  });
});

describe('uploadProjectZip', () => {
  it('usa FormData e não define Content-Type, tratando 204 como sucesso', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 204,
      statusText: 'No Content',
      json: async () => undefined,
      text: async () => '',
    });

    const file = new File(['conteudo'], 'projeto.zip', { type: 'application/zip' });
    await expect(uploadProjectZip('tenant-1', 'p1', file)).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe(`${API_BASE}/v1/projects/p1/upload`);
    expect(init.body).toBeInstanceOf(FormData);
    expect(init.headers['Content-Type']).toBeUndefined();
  });
});
