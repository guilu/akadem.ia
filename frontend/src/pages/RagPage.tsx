import { useEffect, useState } from 'react';
import { getSources, generateQuiz } from '../api';
import type { Subject, SourceDocument, GenerateQuizCommand, GenerateQuizResponse } from '../types';
import SourceUpload from '../components/rag/SourceUpload';
import SourceList from '../components/rag/SourceList';
import QuizGenerateForm from '../components/rag/QuizGenerateForm';
import QuizResults from '../components/rag/QuizResults';
import DraftList from '../components/rag/DraftList';

type Tab = 'sources' | 'generate' | 'drafts';

interface Props {
  token: string;
  subjects: Subject[];
}

export default function RagPage({ token, subjects }: Props) {
  const [tab, setTab] = useState<Tab>('sources');
  const [sources, setSources] = useState<SourceDocument[]>([]);
  const [sourcesLoading, setSourcesLoading] = useState(true);
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null);

  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState('');
  const [result, setResult] = useState<GenerateQuizResponse | null>(null);

  useEffect(() => {
    setSourcesLoading(true);
    getSources(token)
      .then(setSources)
      .catch(() => setSources([]))
      .finally(() => setSourcesLoading(false));
  }, [token]);

  function onUploaded(doc: SourceDocument) {
    setSources((prev) => [doc, ...prev]);
  }

  async function onGenerate(cmd: GenerateQuizCommand) {
    setGenerating(true);
    setGenerateError('');
    setResult(null);
    try {
      const res = await generateQuiz(token, cmd);
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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Generador IA de preguntas</h1>
        <p className="text-sm text-text/50 mt-1">
          Sube documentos PDF y genera preguntas tipo test automáticamente usando RAG.
        </p>
      </div>

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
              <SourceUpload token={token} onUploaded={onUploaded} />
            </section>
            <section>
              <h2 className="text-base font-semibold mb-3">Documentos subidos</h2>
              <SourceList
                sources={sources}
                selectedId={selectedSourceId}
                onSelect={setSelectedSourceId}
                loading={sourcesLoading}
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
                  token={token}
                  sources={sources}
                  subjects={subjects}
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
            <DraftList token={token} sources={sources} />
          </div>
        )}
      </div>
    </div>
  );
}
