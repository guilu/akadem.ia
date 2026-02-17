import { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import type { Question } from './components/ExamRunner';
import Navbar from './components/Navbar';
import { apiBase, apiAuthJson } from './api';
import type { Subject, ExamResult, ExamStartResponse } from './types';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SubjectsPage from './pages/SubjectsPage';
import ExamBuilderPage from './pages/ExamBuilderPage';
import ExamRunnerPage from './pages/ExamRunnerPage';
import ExamAttemptPage from './pages/ExamAttemptPage';
import ExamResultPage from './pages/ExamResultPage';
import SettingsPage from './pages/SettingsPage';
import ProtectedRoute from './pages/ProtectedRoute';

export default function App(){
  const navigate = useNavigate();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [token, setToken] = useState<string>(localStorage.getItem('ak_token') || '');
  const [result, setResult] = useState<ExamResult|null>(null);
  const [activeAttemptId, setActiveAttemptId] = useState<string>(sessionStorage.getItem('akdmia.activeAttemptId') || '');

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

  async function authedJson<T>(url: string, options: RequestInit = {}) {
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
    navigate('/subjects');
  }

  function onLogout(){
    localStorage.removeItem('ak_token');
    sessionStorage.removeItem('akdmia.activeAttemptId');
    setActiveAttemptId('');
    setToken('');
    navigate('/');
  }

  async function startExam(cfg:{ unitCounts: Record<string, number>, minutes: number }){
    const data = await authedJson<ExamStartResponse>(`${apiBase}/api/exams/attempts/start`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify(cfg)
    });
    setAttemptId(data.attemptId);
    setMinutes(Math.round(data.totalTimeSeconds / 60));
    setQuestions(data.questions);
    sessionStorage.setItem('akdmia.activeAttemptId', data.attemptId);
    setActiveAttemptId(data.attemptId);
    navigate(`/exams/attempts/${data.attemptId}`);
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const selections: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) selections[q] = a; });
    const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ selections })
    });
    sessionStorage.removeItem('akdmia.activeAttemptId');
    setActiveAttemptId('');
    setResult(data);
    navigate('/result');
  }

  async function viewResult(attempt: string) {
    const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attempt}/submit`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ selections: {} })
    });
    if (activeAttemptId === attempt) {
      sessionStorage.removeItem('akdmia.activeAttemptId');
      setActiveAttemptId('');
    }
    setResult(data);
    navigate('/result');
  }

  function resumeAttempt(attempt: string) {
    sessionStorage.setItem('akdmia.activeAttemptId', attempt);
    setActiveAttemptId(attempt);
    navigate(`/exams/attempts/${attempt}`);
  }

  return (
    <div className="min-h-screen">
      <Navbar
        isAuthed={isAuthed}
        isAdmin={role === 'ADMIN'}
        onLogout={onLogout}
      />

      <main className="max-w-4xl mx-auto p-6">
        <Routes>
          <Route path="/" element={<HomePage isAuthed={isAuthed} activeAttemptId={activeAttemptId} />} />
          <Route path="/login" element={<LoginPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path="/register" element={<RegisterPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path="/subjects" element={
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
          <Route path="/exam" element={
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
          <Route path="/result" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamResultPage result={result} />
            </ProtectedRoute>
          } />
          <Route path="/settings" element={<SettingsPage isAdmin={role === 'ADMIN'} token={token} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
