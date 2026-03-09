import { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom';
import type { Question } from './components/ExamRunner';
import Navbar from './components/Navbar';
import { apiBase, apiAuthJson } from './api';
import type { Subject, ExamResult, ExamStartResponse } from './types';
import { timeoutMessage } from './utils/messages';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SubjectsPage from './pages/SubjectsPage';
import ExamBuilderPage from './pages/ExamBuilderPage';
import ExamRunnerPage from './pages/ExamRunnerPage';
import ExamAttemptPage from './pages/ExamAttemptPage';
import ExamResultPage from './pages/ExamResultPage';
import SettingsPage from './pages/SettingsPage';
import FlashcardsPage from './pages/FlashcardsPage';
import FlashcardsStudyPage from './pages/FlashcardsStudyPage';
import RagPage from './pages/RagPage';
import ProtectedRoute from './pages/ProtectedRoute';
import { ROUTES } from './constants/routes';

export default function App(){
  const navigate = useNavigate();
  const location = useLocation();
  const isHome = location.pathname === ROUTES.home;
  const isFullWidth = isHome || location.pathname === ROUTES.login || location.pathname === ROUTES.register;
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [token, setToken] = useState<string>(localStorage.getItem('ak_token') || '');
  const [result, setResult] = useState<ExamResult|null>(null);
  const [activeAttemptId, setActiveAttemptId] = useState<string>(sessionStorage.getItem('akdmia.activeAttemptId') || '');
  const [toastError, setToastError] = useState<string>('');

  function showError(msg: string) {
    setToastError(msg);
    setTimeout(() => setToastError(''), 5000);
  }

  const isAuthed = useMemo(() => Boolean(token), [token]);
  const role = useMemo(() => {
    try {
      if (!token) return null;
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || null;
    } catch {
      return null;
    }
  }, [token]);

  async function authedJson<T>(url: string, options: RequestInit & { timeoutMs?: number } = {}) {
    try {
      return await apiAuthJson<T>(url, token, options);
    } catch (err: any) {
      if (err?.status === 401) onLogout();
      throw err;
    }
  }

  useEffect(() => {
    if (!token) {
      setSubjects([]);
      return;
    }
    authedJson<Subject[]>(`${apiBase}/api/subjects`)
      .then(setSubjects)
      .catch(() => setSubjects([]));
  }, [token, apiBase]);

  function onToken(t:string){
    setToken(t);
    localStorage.setItem('ak_token', t);
    navigate(ROUTES.subjects);
  }

  function onLogout(){
    localStorage.removeItem('ak_token');
    sessionStorage.removeItem('akdmia.activeAttemptId');
    setActiveAttemptId('');
    setToken('');
    navigate(ROUTES.home);
  }

  // timeoutMessage from utils
  async function startExam(cfg:{ unitCounts: Record<string, number>, minutes: number, difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }){
    try {
      const data = await authedJson<ExamStartResponse>(`${apiBase}/api/exams/attempts/start`, {
        method: 'POST',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify(cfg),
        timeoutMs: 15000
      });
      setAttemptId(data.attemptId);
      setMinutes(Math.round(data.totalTimeSeconds / 60));
      setQuestions(data.questions);
      sessionStorage.setItem('akdmia.activeAttemptId', data.attemptId);
      setActiveAttemptId(data.attemptId);
      navigate(ROUTES.examAttempt(data.attemptId));
    } catch (err: any) {
      if (err?.code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const selections: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) selections[q] = a; });
    try {
      const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
        method: 'POST',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify({ selections }),
        timeoutMs: 15000
      });
      sessionStorage.removeItem('akdmia.activeAttemptId');
      setActiveAttemptId('');
      setResult(data);
      navigate(ROUTES.examResult);
    } catch (err: any) {
      if (err?.code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  async function viewResult(attempt: string) {
    try {
      const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attempt}/submit`, {
        method: 'POST',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify({ selections: {} }),
        timeoutMs: 15000
      });
      if (activeAttemptId === attempt) {
        sessionStorage.removeItem('akdmia.activeAttemptId');
        setActiveAttemptId('');
      }
      setResult(data);
      navigate(ROUTES.examResult);
    } catch (err: any) {
      if (err?.code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  function resumeAttempt(attempt: string) {
    sessionStorage.setItem('akdmia.activeAttemptId', attempt);
    setActiveAttemptId(attempt);
    navigate(ROUTES.examAttempt(attempt));
  }

  return (
    <div className="min-h-screen">
      <Navbar
        isAuthed={isAuthed}
        isAdmin={role === 'ADMIN'}
        onLogout={onLogout}
      />

      {toastError && (
        <div role="alert" className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-red-600 text-white px-4 py-2 rounded-lg shadow-lg text-sm">
          {toastError}
        </div>
      )}
      <main className={isFullWidth ? 'pt-[4rem] overflow-x-hidden' : 'max-w-4xl mx-auto p-6 pt-24'}>
        <Routes>
          <Route path={ROUTES.home} element={<HomePage isAuthed={isAuthed} activeAttemptId={activeAttemptId} />} />
          <Route path={ROUTES.login} element={<LoginPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path={ROUTES.register} element={<RegisterPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path={ROUTES.subjects} element={
            <ProtectedRoute allow={isAuthed}>
              <SubjectsPage
                subjects={subjects}
                activeAttemptId={activeAttemptId}
                token={token}
                onUnauthorized={onLogout}
                onViewResult={viewResult}
                onResumeAttempt={resumeAttempt}
              />
            </ProtectedRoute>
          } />
          <Route path="/subjects/:subjectId/builder" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamBuilderPage onStart={startExam} onUnauthorized={onLogout} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.examRunner} element={
            <ProtectedRoute allow={isAuthed}>
              <ExamRunnerPage questions={questions} minutes={minutes} onFinish={finishExam} />
            </ProtectedRoute>
          } />
          <Route path="/exams/attempts/:attemptId" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamAttemptPage token={token} onUnauthorized={onLogout} onFinish={(res) => {
                sessionStorage.removeItem('akdmia.activeAttemptId');
                setActiveAttemptId('');
                setResult(res);
              }} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.examResult} element={
            <ProtectedRoute allow={isAuthed}>
              <ExamResultPage result={result} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.settings} element={
            <ProtectedRoute allow={isAuthed}>
              <SettingsPage isAdmin={role === 'ADMIN'} token={token} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.flashcards} element={
            <ProtectedRoute allow={isAuthed}>
              <FlashcardsPage />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.flashcardsStudy} element={
            <ProtectedRoute allow={isAuthed}>
              <FlashcardsStudyPage />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.rag} element={
            <ProtectedRoute allow={isAuthed && role === 'ADMIN'}>
              <RagPage token={token} subjects={subjects} />
            </ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to={ROUTES.home} replace />} />
        </Routes>
      </main>
    </div>
  );
}
