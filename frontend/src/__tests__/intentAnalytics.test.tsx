import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SyllabusesPage from '../pages/SyllabusesPage';
import SyllabusSubjectsPage from '../pages/SyllabusSubjectsPage';

const { trackEventMock, getSyllabusesMock, getSubjectsInSyllabusMock, apiJsonMock } = vi.hoisted(() => ({
  trackEventMock: vi.fn(),
  getSyllabusesMock: vi.fn(),
  getSubjectsInSyllabusMock: vi.fn(),
  apiJsonMock: vi.fn(),
}));

vi.mock('../lib/analytics', () => ({ trackEvent: trackEventMock }));
vi.mock('../api/syllabusApi', () => ({
  getSyllabuses: getSyllabusesMock,
  getSubjectsInSyllabus: getSubjectsInSyllabusMock,
}));
vi.mock('../api', () => ({
  apiBase: 'http://localhost:8080',
  apiJson: apiJsonMock,
}));

describe('curriculum intent analytics', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiJsonMock.mockResolvedValue([]);
  });

  it('tracks curriculum_view only after the syllabus catalogue loads', async () => {
    getSyllabusesMock.mockResolvedValue([
      { id: 'syllabus-1', name: 'Subalterno', description: '', visibility: 'GLOBAL' },
    ]);

    render(
      <MemoryRouter>
        <SyllabusesPage
          onUnauthorized={vi.fn()}
          onViewResult={vi.fn()}
          onResumeAttempt={vi.fn()}
        />
      </MemoryRouter>,
    );

    await screen.findByText('Subalterno');
    expect(trackEventMock).toHaveBeenCalledWith('curriculum_view', {
      syllabus_count: 1,
    });
  });

  it('does not track curriculum_view when the catalogue fails', async () => {
    getSyllabusesMock.mockRejectedValue(new Error('offline'));
    render(
      <MemoryRouter>
        <SyllabusesPage
          onUnauthorized={vi.fn()}
          onViewResult={vi.fn()}
          onResumeAttempt={vi.fn()}
        />
      </MemoryRouter>,
    );
    await screen.findByText('No se pudieron cargar los temarios.');
    expect(trackEventMock).not.toHaveBeenCalledWith('curriculum_view', expect.anything());
  });

  it('tracks topic_view with opaque ids when a topic is opened', async () => {
    getSubjectsInSyllabusMock.mockResolvedValue([
      { id: 'subject-1', name: 'Constitución', description: '' },
    ]);

    render(
      <MemoryRouter initialEntries={['/syllabuses/syllabus-1/subjects']}>
        <Routes>
          <Route path="/syllabuses/:syllabusId/subjects" element={<SyllabusSubjectsPage />} />
          <Route path="/subjects/:subjectId/builder" element={<div>Builder</div>} />
        </Routes>
      </MemoryRouter>,
    );

    const openTopic = await screen.findByRole('link', { name: /crear examen/i });
    fireEvent.click(openTopic);
    await waitFor(() => {
      expect(trackEventMock).toHaveBeenCalledWith('topic_view', {
        syllabus_id: 'syllabus-1',
        subject_id: 'subject-1',
      });
    });
  });
});
