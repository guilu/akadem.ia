import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowUpFromBracket, Close, CheckCircle, CirclePlus, Refresh, Clock, Inbox } from 'flowbite-react-icons/outline';
import { apiJson, apiBase, exportFlashcardsBySubject } from '../api';
import FlashcardImportModal from '../components/flashcards/FlashcardImportModal';
import FlashcardsTabs from '../components/flashcards/FlashcardsTabs';
import SearchInput from '../components/flashcards/SearchInput';
import SubjectCard from '../components/flashcards/SubjectCard';
import type { SubjectSummary } from '../components/flashcards/SubjectCard';
import UnitList from '../components/flashcards/UnitList';

export type UnitSummary = {
  unitId: string;
  unitName: string;
  subjectId: string;
  subjectName: string;
  newCount: number;
  reviewCount: number;
  dueCount?: number;
};

type GlobalQueue = { new: number; due: number; learning: number };

export default function FlashcardsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') === 'study' ? 'estudio' : 'examinar';

  const [units, setUnits] = useState<UnitSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [globalQueue, setGlobalQueue] = useState<GlobalQueue | null>(null);
  const [globalLoading, setGlobalLoading] = useState(true);
  const [showImport, setShowImport] = useState(false);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | null>(null);
  const [exportError, setExportError] = useState<string>('');

  const loadUnits = () => {
    setLoading(true);
    setError('');
    apiJson<UnitSummary[]>(`${apiBase}/api/flashcards/units/summary`)
      .then((data) => setUnits(data || []))
      .catch(() => setError('No se pudieron cargar las unidades.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError('');
    apiJson<UnitSummary[]>(`${apiBase}/api/flashcards/units/summary`)
      .then((data) => { if (mounted) setUnits(data || []); })
      .catch(() => { if (mounted) setError('No se pudieron cargar las unidades.'); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    let mounted = true;
    setGlobalLoading(true);
    apiJson<GlobalQueue>(`${apiBase}/api/flashcards/study/queue`)
      .then((data) => { if (mounted) setGlobalQueue(data || null); })
      .catch(() => {})
      .finally(() => { if (mounted) setGlobalLoading(false); });
    return () => { mounted = false; };
  }, []);

  // Derive subject summaries from unit data
  const subjects = useMemo<SubjectSummary[]>(() => {
    const map = new Map<string, SubjectSummary>();
    for (const u of units) {
      if (!u.subjectId) continue;
      if (!map.has(u.subjectId)) {
        map.set(u.subjectId, {
          subjectId: u.subjectId,
          subjectName: u.subjectName,
          newCount: 0,
          reviewCount: 0,
          dueCount: 0,
          unitCount: 0,
        });
      }
      const s = map.get(u.subjectId)!;
      s.newCount += u.newCount;
      s.reviewCount += u.reviewCount;
      s.dueCount += u.dueCount ?? 0;
      s.unitCount++;
    }
    return Array.from(map.values());
  }, [units]);

  const selectedSubject = selectedSubjectId
    ? subjects.find((s) => s.subjectId === selectedSubjectId) ?? null
    : null;

  const unitsForSubject = useMemo(
    () => (selectedSubjectId ? units.filter((u) => u.subjectId === selectedSubjectId) : []),
    [units, selectedSubjectId]
  );

  const filteredSubjects = useMemo(() => {
    const q = search.trim().toLowerCase();
    return q ? subjects.filter((s) => s.subjectName.toLowerCase().includes(q)) : subjects;
  }, [subjects, search]);

  const filteredUnitsForSubject = useMemo(() => {
    const q = search.trim().toLowerCase();
    return q ? unitsForSubject.filter((u) => u.unitName.toLowerCase().includes(q)) : unitsForSubject;
  }, [unitsForSubject, search]);

  const totalPending = globalQueue ? globalQueue.new + globalQueue.due + globalQueue.learning : 0;

  const handleUnitClick = (unit: UnitSummary) => {
    if (mode === 'estudio') {
      navigate(`/flashcards/study?unitId=${unit.unitId}`);
    } else {
      navigate(`/flashcards/examine?unitId=${unit.unitId}`);
    }
  };

  const handleExport = async (unit: UnitSummary, format: 'csv' | 'json') => {
    setExportError('');
    try {
      const res = await fetch(
        `${apiBase}/api/flashcards/export?unitId=${unit.unitId}&format=${format}`,
        { credentials: 'include' }
      );
      if (!res.ok) {
        setExportError('No se pudo exportar. Inténtalo de nuevo.');
        return;
      }
      const content = await res.text();
      const blob = new Blob([content], {
        type: format === 'json' ? 'application/json' : 'text/csv',
      });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${unit.unitName}.${format}`;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch {
      setExportError('No se pudo exportar. Inténtalo de nuevo.');
    }
  };

  const handleSubjectExport = async (subject: SubjectSummary, format: 'csv' | 'json') => {
    setExportError('');
    try {
      const content = await exportFlashcardsBySubject(subject.subjectId, format);
      const blob = new Blob([content], {
        type: format === 'json' ? 'application/json' : 'text/csv',
      });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${subject.subjectName}.${format}`;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch {
      setExportError('No se pudo exportar la materia. Inténtalo de nuevo.');
    }
  };

  const handleSelectSubject = (subjectId: string) => {
    setSelectedSubjectId(subjectId);
    setSearch('');
  };

  const handleBack = () => {
    setSelectedSubjectId(null);
    setSearch('');
  };

  const openImport = () => setShowImport(true);

  return (
    <div className="space-y-6">
      <header className="space-y-3">
        <div className="py-[1.5rem] flex items-start justify-between">
          <div className="flex items-center gap-3">
            {selectedSubjectId && (
              <button
                type="button"
                onClick={handleBack}
                className="flex items-center justify-center w-8 h-8 rounded-xl border border-secondary/30 text-text/60 hover:border-primary/40 hover:text-text transition-colors"
                aria-label="Volver a materias"
              >
                <ArrowLeft className="w-4 h-4" />
              </button>
            )}
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight">
                Flash
                <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                  cards
                </span>
              </h1>
              {selectedSubject ? (
                <p className="text-text/55 text-sm mt-1">{selectedSubject.subjectName}</p>
              ) : (
                <p className="text-text/55 text-sm mt-1">Repasa por unidades con repetición espaciada.</p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={openImport}
            className="mt-1 flex items-center gap-1.5 px-3 py-2 rounded-xl border border-secondary/30 text-sm text-text/60 hover:border-primary/40 hover:text-text transition-colors"
          >
            <ArrowUpFromBracket className="w-4 h-4" />
            Importar
          </button>
        </div>
        <FlashcardsTabs active={mode} onTab={(tab) => {
          if (tab === 'estudio') navigate('/flashcards?mode=study');
          if (tab === 'examinar') navigate('/flashcards');
          if (tab === 'historial') navigate('/flashcards/history');
        }} />
      </header>

      {/* Export error banner */}
      {exportError && (
        <div
          role="alert"
          className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400 flex items-center justify-between gap-3"
        >
          <span>{exportError}</span>
          <button
            type="button"
            onClick={() => setExportError('')}
            className="text-red-400/70 hover:text-red-400 transition-colors shrink-0"
            aria-label="Cerrar error"
          >
            <Close className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Global queue strip — only in study mode and subject list view */}
      {mode === 'estudio' && !selectedSubjectId && (
        globalLoading ? (
          <div className="h-12 rounded-2xl border border-secondary/15 bg-secondary/5 animate-pulse" />
        ) : globalQueue && totalPending === 0 ? (
          <div className="border border-secondary/25 rounded-2xl px-5 py-3 flex items-center justify-center gap-2 text-sm text-text/60">
            <CheckCircle className="w-4 h-4 text-lime-500" />
            Nada pendiente hoy
          </div>
        ) : globalQueue ? (
          <div className="border border-secondary/25 rounded-2xl px-5 py-3 flex items-center justify-around text-sm font-medium">
            <span className="flex items-center gap-1.5 text-lime-500">
              <CirclePlus className="w-4 h-4" />
              {globalQueue.new} nuevas
            </span>
            <span className="flex items-center gap-1.5 text-accent">
              <Refresh className="w-4 h-4" />
              {globalQueue.learning} aprendiendo
            </span>
            <span className="flex items-center gap-1.5 text-primary">
              <Clock className="w-4 h-4" />
              {globalQueue.due} pendientes
            </span>
          </div>
        ) : null
      )}

      <section className="space-y-4">
        <SearchInput value={search} onChange={setSearch} />

        {selectedSubjectId ? (
          <UnitList
            loading={loading}
            error={error}
            units={filteredUnitsForSubject}
            onUnitClick={handleUnitClick}
            onExport={handleExport}
            onImport={openImport}
          />
        ) : loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-20 rounded-2xl border border-secondary/15 bg-secondary/5 animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">
            {error}
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="border border-secondary/25 rounded-2xl px-5 py-8 text-center space-y-4">
            <Inbox className="w-8 h-8 mx-auto text-text/25" />
            <p className="text-sm text-text/55">No hay materias disponibles.</p>
            <button
              type="button"
              onClick={openImport}
              className="btn btn-primary rounded-full px-6 py-2 text-sm shadow-sm shadow-primary/15"
            >
              Importar flashcards
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredSubjects.map((subject) => (
              <SubjectCard
                key={subject.subjectId}
                subject={subject}
                onClick={() => handleSelectSubject(subject.subjectId)}
                onExport={(fmt) => handleSubjectExport(subject, fmt)}
              />
            ))}
          </div>
        )}
      </section>

      {showImport && (
        <FlashcardImportModal
          onClose={() => setShowImport(false)}
          onImported={loadUnits}
        />
      )}
    </div>
  );
}
