import { apiBase, apiJson } from '../api';
import type { Syllabus, Subject, ContentVisibility } from '../types';

export async function getSyllabuses(): Promise<Syllabus[]> {
  return apiJson<Syllabus[]>(`${apiBase}/api/syllabuses`);
}

export async function getSyllabus(id: string): Promise<Syllabus> {
  return apiJson<Syllabus>(`${apiBase}/api/syllabuses/${id}`);
}

export async function getSubjectsInSyllabus(syllabusId: string): Promise<Subject[]> {
  return apiJson<Subject[]>(`${apiBase}/api/syllabuses/${syllabusId}/subjects`);
}

export async function createSyllabus(data: {
  name: string;
  description?: string;
  visibility?: ContentVisibility;
}): Promise<Syllabus> {
  return apiJson<Syllabus>(`${apiBase}/api/syllabuses`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export async function updateSyllabus(
  id: string,
  data: { name: string; description?: string }
): Promise<Syllabus> {
  return apiJson<Syllabus>(`${apiBase}/api/manage/syllabuses/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export async function deleteSyllabus(id: string): Promise<void> {
  return apiJson<void>(`${apiBase}/api/syllabuses/${id}`, { method: 'DELETE' });
}
