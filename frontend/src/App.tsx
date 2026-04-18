import { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom';
import type { Question } from './components/ExamRunner';
import Navbar from './components/Navbar';
import { apiBase, apiJson, getMe, logout } from './api';
import type { Subject, ExamResult, ExamStartResponse, NavUser } from './types';
import { timeoutMessage } from './utils/messages';
import { deriveInitials } from './utils/format';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SubjectsPage from './pages/SubjectsPage';
import SyllabusesPage from './pages/SyllabusesPage';
import SyllabusSubjectsPage from './pages/SyllabusSubjectsPage';
import SyllabusExamBuilderPage from './pages/SyllabusExamBuilderPage';
import ExamBuilderPage from './pages/ExamBuilderPage';
import ExamRunnerPage from './pages/ExamRunnerPage';
import ExamAttemptPage from './pages/ExamAttemptPage';
import ExamResultPage from './pages/ExamResultPage';
import SettingsPage from './pages/SettingsPage';
import ManagePage from './pages/ManagePage';
import FlashcardsPage from './pages/FlashcardsPage';
import FlashcardsStudyPage from './pages/FlashcardsStudyPage';
import FlashcardsHistoryPage from './pages/FlashcardsHistoryPage';
import FlashcardsExamineUnitPage from './pages/FlashcardsExamineUnitPage';
import RagPage from './pages/RagPage';
import ProfilePage from './pages/ProfilePage';
import ProtectedRoute from './pages/ProtectedRoute';
import { ROUTES } from './constants/routes';

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const isHome = location.pathname === ROUTES.home;
  const isFullWidth = isHome || location.pathname === ROUTES.login || location.pathname === ROUTES.register;
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [authUser, setAuthUser] = useState<{ email: string; role: string } | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [result, setResult] = useState<ExamResult | null>(null);
  const [activeAttemptId, setActiveAttemptId] = useState<string>(sessionStorage.getItem('akdmia.activeAttemptId') || '');
  const [toastError, setToastError] = useState<string>('');

  function showError(msg: string) {
    setToastError(msg);
    setTimeout(() => setToastError(''), 5000);
  }

  // On mount: check if there is a valid session via cookie
  useEffect(() => {
    getMe()
      .then((data) => setAuthUser(data))
      .catch(() => setAuthUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  const isAuthed = Boolean(authUser) && !authLoading;

  const { role, user } = useMemo<{ role: string | null; user: NavUser | null }>(() => {
    if (!authUser) return { role: null, user: null };
    const email = authUser.email;
    const initials = deriveInitials(email);
    return { role: authUser.role || null, user: { email, initials } };
  }, [authUser]);

  async function authedJson<T>(url: string, options: RequestInit & { timeoutMs?: number } = {}) {
    try {
      return await apiJson<T>(url, options);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) {
        onLogout();
      }
      throw err;
    }
  }

  const refreshSubjects = () => {
    if (!authUser) {
      setSubjects([]);
      return Promise.resolve();
    }
    return authedJson<Subject[]>(`${apiBase}/api/subjects`)
      .then(setSubjects)
      .catch(() => setSubjects([]));
  };

  useEffect(() => {
    refreshSubjects();
  }, [authUser, apiBase]);

  // Handle OAuth2 callback: exchange ephemeral code for JWT cookie
  useEffect(() => {
    if (location.pathname === ROUTES.oauth2Callback) {
      const params = new URLSearchParams(location.search);
      const code = params.get('code');
      if (!code) {
        navigate(ROUTES.login);
        return;
      }
      fetch(`${apiBase}/api/auth/oauth2/exchange`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code }),
        credentials: 'include',
      })
        .then((res) => {
          if (!res.ok) throw new Error('exchange_failed');
          return getMe();
        })
        .then((data) => onAuthSuccess(data))
        .catch(() => navigate(ROUTES.login));
    }
  }, [location.pathname]);

  function onAuthSuccess(userData: { email: string; role: string }) {
    setAuthUser(userData);
    navigate(ROUTES.syllabuses);
  }

  function onLogout() {
    logout().catch(() => {});
    sessionStorage.removeItem('akdmia.activeAttemptId');
    setActiveAttemptId('');
    setAuthUser(null);
    navigate(ROUTES.home);
  }

  // timeoutMessage from utils
  async function startExam(cfg: { unitCounts: Record<string, number>; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) {
    try {
      const data = await authedJson<ExamStartResponse>(`${apiBase}/api/exams/attempts/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cfg),
        timeoutMs: 15000
      });
      setAttemptId(data.attemptId);
      setMinutes(Math.round(data.totalTimeSeconds / 60));
      setQuestions(data.questions);
      sessionStorage.setItem('akdmia.activeAttemptId', data.attemptId);
      setActiveAttemptId(data.attemptId);
      navigate(ROUTES.examAttempt(data.attemptId));
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  async function startRandomExam(cfg: { subjectId: string; count: number; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) {
    try {
      const data = await authedJson<ExamStartResponse>(`${apiBase}/api/exams/attempts/start-random`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cfg),
        timeoutMs: 15000
      });
      setAttemptId(data.attemptId);
      setMinutes(Math.round(data.totalTimeSeconds / 60));
      setQuestions(data.questions);
      sessionStorage.setItem('akdmia.activeAttemptId', data.attemptId);
      setActiveAttemptId(data.attemptId);
      navigate(ROUTES.examAttempt(data.attemptId));
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  async function finishExam(payload: { selections: Record<string, string | undefined> }) {
    const selections: Record<string, string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => {
      if (a) {
        selections[q] = a;
      }
    });
    try {
      const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ selections }),
        timeoutMs: 15000
      });
      sessionStorage.removeItem('akdmia.activeAttemptId');
      setActiveAttemptId('');
      setResult(data);
      navigate(ROUTES.examResult);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
        showError(timeoutMessage);
        return;
      }
      throw err;
    }
  }

  async function viewResult(attempt: string) {
    try {
      const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attempt}/result`, {
        timeoutMs: 15000
      });
      if (activeAttemptId === attempt) {
        sessionStorage.removeItem('akdmia.activeAttemptId');
        setActiveAttemptId('');
      }
      setResult(data);
      navigate(ROUTES.examResult);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
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

  // While session is being verified, don't redirect protected routes
  if (authLoading) {
    return null;
  }

  return (
    <div className="min-h-screen">
      <Navbar
        isAuthed={isAuthed}
        isAdmin={role === 'ADMIN'}
        user={user}
        onLogout={onLogout}
        onProfile={() => navigate(ROUTES.profile)}
        onSettings={() => navigate(ROUTES.settings)}
      />

      {toastError && (
        <div role="alert" className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-red-600 text-white px-4 py-2 rounded-lg shadow-lg text-sm">
          {toastError}
        </div>
      )}
      <main className={isFullWidth ? 'pt-[4rem] overflow-x-hidden' : 'max-w-7xl mx-auto p-6 pt-24'}>
        <Routes>
          <Route path={ROUTES.home} element={<HomePage isAuthed={isAuthed} activeAttemptId={activeAttemptId} />} />
          <Route path={ROUTES.login} element={<LoginPage isAuthed={isAuthed} onAuthSuccess={onAuthSuccess} />} />
          <Route path={ROUTES.register} element={<RegisterPage isAuthed={isAuthed} onAuthSuccess={onAuthSuccess} />} />
          <Route path={ROUTES.syllabuses} element={
            <ProtectedRoute allow={isAuthed}>
              <SyllabusesPage
                activeAttemptId={activeAttemptId}
                onUnauthorized={onLogout}
                onViewResult={viewResult}
                onResumeAttempt={resumeAttempt}
              />
            </ProtectedRoute>
          } />
          <Route path="/syllabuses/:syllabusId/subjects" element={
            <ProtectedRoute allow={isAuthed}>
              <SyllabusSubjectsPage />
            </ProtectedRoute>
          } />
          <Route path="/syllabuses/:syllabusId/exam-builder" element={
            <ProtectedRoute allow={isAuthed}>
              <SyllabusExamBuilderPage onUnauthorized={onLogout} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.subjects} element={
            <ProtectedRoute allow={isAuthed}>
              <SubjectsPage
                subjects={subjects}
                activeAttemptId={activeAttemptId}
                onUnauthorized={onLogout}
                onViewResult={viewResult}
                onResumeAttempt={resumeAttempt}
              />
            </ProtectedRoute>
          } />
          <Route path="/subjects/:subjectId/builder" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamBuilderPage onStart={startExam} onStartRandom={startRandomExam} onUnauthorized={onLogout} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.examRunner} element={
            <ProtectedRoute allow={isAuthed}>
              <ExamRunnerPage questions={questions} minutes={minutes} onFinish={finishExam} />
            </ProtectedRoute>
          } />
          <Route path="/exams/attempts/:attemptId" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamAttemptPage onUnauthorized={onLogout} onFinish={(res) => {
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
              <SettingsPage isAdmin={role === 'ADMIN'} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.manage} element={
            <ProtectedRoute allow={isAuthed}>
              <ManagePage isAdmin={role === 'ADMIN'} onSubjectsChanged={refreshSubjects} />
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
          <Route path={ROUTES.flashcardsHistory} element={
            <ProtectedRoute allow={isAuthed}>
              <FlashcardsHistoryPage />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.flashcardsExamine} element={
            <ProtectedRoute allow={isAuthed}>
              <FlashcardsExamineUnitPage />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.rag} element={
            <ProtectedRoute allow={isAuthed && role === 'ADMIN'}>
              <RagPage subjects={subjects} />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.profile} element={
            <ProtectedRoute allow={isAuthed}>
              <ProfilePage />
            </ProtectedRoute>
          } />
          <Route path={ROUTES.oauth2Callback} element={null} />
          <Route path="*" element={<Navigate to={ROUTES.home} replace />} />
        </Routes>
      </main>
    </div>
  );
}
