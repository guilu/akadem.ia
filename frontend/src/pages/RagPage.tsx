import { useEffect, useState } from 'react';
import { getSources, generateQuiz, deleteSource } from '../api';
import type { Subject, SourceDocument, GenerateQuizCommand, GenerateQuizResponse } from '../types';
import SourceUpload from '../components/rag/SourceUpload';
import SourceList from '../components/rag/SourceList';
import QuizGenerateForm from '../components/rag/QuizGenerateForm';
import QuizResults from '../components/rag/QuizResults';
import DraftList from '../components/rag/DraftList';

type Tab = 'sources' | 'generate' | 'drafts';

interface Props {
  subjects: Subject[];
}

export default function RagPage({ subjects }: Props) {
  const [tab, setTab] = useState<Tab>('sources');
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [sources, setSources] = useState<SourceDocument[]>([]);
  const [sourcesLoading, setSourcesLoading] = useState(false);

  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState('');
  const [result, setResult] = useState<GenerateQuizResponse | null>(null);

  useEffect(() => {
    if (!selectedSubjectId) { setSources([]); return; }
    setSourcesLoading(true);
    getSources(selectedSubjectId)
      .then(setSources)
      .catch(() => setSources([]))
      .finally(() => setSourcesLoading(false));
  }, [selectedSubjectId]);

  function onUploaded(doc: SourceDocument) {
    setSources((prev) => [doc, ...prev]);
  }

  async function onDelete(id: string) {
    await deleteSource(id);
    setSources((prev) => prev.filter((s) => s.id !== id));
  }

  async function onGenerate(cmd: GenerateQuizCommand) {
    setGenerating(true);
    setGenerateError('');
    setResult(null);
    try {
      const res = await generateQuiz(cmd);
      setResult(res);
    } catch (err: any) {
      if (err?.code === 'timeout') {
        setGenerateError('La generación tardó demasiado. Intenta con menos preguntas.');
      } else {
        setGenerateError(err?.body?.message || 'Error al generar las preguntas.');
      }
    } finally {
      setGenerating(false);
    }
  }

  const tabCls = (t: Tab) =>
    `px-5 py-2.5 text-sm font-medium rounded-xl transition-colors ${
      tab === t
        ? 'bg-primary text-white'
        : 'text-text/60 hover:text-text hover:bg-secondary/10'
    }`;

  const inputCls = 'rounded-xl border border-secondary/25 bg-secondary/5 px-3 py-2 text-sm focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/30';

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold">Generador IA de preguntas</h1>
          <p className="text-sm text-text/50 mt-1">
            Sube documentos PDF y genera preguntas tipo test automáticamente usando RAG.
          </p>
        </div>

        {/* Global subject selector */}
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-text/70 whitespace-nowrap">Asignatura:</label>
          <select
            value={selectedSubjectId}
            onChange={(e) => {
              setSelectedSubjectId(e.target.value);
              setResult(null);
              setGenerateError('');
            }}
            className={inputCls}
          >
            <option value="">— Selecciona asignatura —</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>
      </div>

      {!selectedSubjectId && (
        <div className="border border-secondary/25 rounded-2xl px-5 py-8 text-center">
          <p className="text-sm text-text/55">Selecciona una asignatura para comenzar.</p>
        </div>
      )}

      {selectedSubjectId && (
        <>
          <div className="flex gap-2 p-1 bg-secondary/10 rounded-xl w-fit">
            <button className={tabCls('sources')} onClick={() => setTab('sources')}>
              Fuentes
            </button>
            <button className={tabCls('generate')} onClick={() => setTab('generate')}>
              Generar
            </button>
            <button className={tabCls('drafts')} onClick={() => setTab('drafts')}>
              Borradores
            </button>
          </div>

          <div className="rounded-2xl border border-secondary/20 bg-bg p-6">
            {tab === 'sources' && (
              <div className="space-y-6">
                <section>
                  <h2 className="text-base font-semibold mb-3">Subir documento PDF</h2>
                  <SourceUpload
                    subjectId={selectedSubjectId}
                    onUploaded={onUploaded}
                  />
                </section>
                <section>
                  <h2 className="text-base font-semibold mb-3">Documentos subidos</h2>
                  <SourceList
                    sources={sources}
                    selectedId={null}
                    onSelect={() => {}}
                    loading={sourcesLoading}
                    onDelete={onDelete}
                  />
                </section>
              </div>
            )}

            {tab === 'generate' && (
              <div className="space-y-6">
                {result ? (
                  <QuizResults result={result} onReset={() => setResult(null)} />
                ) : (
                  <>
                    <h2 className="text-base font-semibold">Configurar generación</h2>
                    <QuizGenerateForm
                      sources={sources}
                      subjectId={selectedSubjectId}
                      onGenerate={onGenerate}
                      loading={generating}
                    />
                    {generateError && (
                      <p role="alert" className="text-sm text-red-500">{generateError}</p>
                    )}
                  </>
                )}
              </div>
            )}

            {tab === 'drafts' && (
              <div className="space-y-6">
                <h2 className="text-base font-semibold">Borradores guardados</h2>
                <DraftList sources={sources} />
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
