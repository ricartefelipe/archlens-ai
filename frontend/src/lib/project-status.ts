import type { Project } from './types';

export function analysisBlockReason(project: Project): string | null {
  if (project.fileCount <= 0 || project.status === 'CREATED') {
    return 'Envie um arquivo ZIP antes de rodar a análise.';
  }
  if (project.status === 'UPLOADING') {
    return 'Upload em andamento. Aguarde concluir antes de analisar.';
  }
  if (project.status === 'FAILED') {
    return 'Upload ou indexação falhou. Envie o ZIP novamente na aba Upload.';
  }
  return null;
}

export function isProjectBusy(project: Project): boolean {
  return project.status === 'UPLOADING' || project.status === 'INGESTING';
}

export function projectBusyMessage(project: Project): string | null {
  if (project.status === 'UPLOADING') {
    return 'Enviando e extraindo arquivos…';
  }
  if (project.status === 'INGESTING') {
    return 'Indexando arquivos para busca e chat. A análise estática já pode rodar.';
  }
  return null;
}
