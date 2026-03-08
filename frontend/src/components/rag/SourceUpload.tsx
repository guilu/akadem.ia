import { useRef, useState } from 'react';
import { uploadSource } from '../../api';
import type { SourceDocument } from '../../types';

interface Props {
  token: string;
  onUploaded: (doc: SourceDocument) => void;
}

export default function SourceUpload({ token, onUploaded }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [dragOver, setDragOver] = useState(false);

  async function handleFile(file: File) {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setError('Solo se admiten archivos PDF.');
      return;
    }
    if (file.size > 50 * 1024 * 1024) {
      setError('El archivo no puede superar 50 MB.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const doc = await uploadSource(token, file);
      onUploaded(doc);
    } catch (err: any) {
      setError(err?.body?.message || 'Error al subir el documento.');
    } finally {
      setLoading(false);
    }
  }

  function onInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    e.target.value = '';
  }

  function onDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  return (
    <div className="space-y-4">
      <div
        role="button"
        tabIndex={0}
        aria-label="Zona de subida de archivo PDF"
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        className={`border-2 border-dashed rounded-xl p-10 text-center cursor-pointer transition-colors ${
          dragOver
            ? 'border-primary bg-primary/5'
            : 'border-secondary/30 hover:border-primary/50 hover:bg-secondary/5'
        }`}
      >
        <svg className="mx-auto mb-3 w-10 h-10 text-secondary/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16v1a2 2 0 002 2h12a2 2 0 002-2v-1M12 12V4m0 0L8 8m4-4l4 4" />
        </svg>
        {loading ? (
          <p className="text-sm text-text/60">Subiendo y procesando…</p>
        ) : (
          <>
            <p className="text-sm font-medium text-text/70">Arrastra un PDF aquí o haz clic para seleccionar</p>
            <p className="text-xs text-text/40 mt-1">Máximo 50 MB · Solo PDF</p>
          </>
        )}
        <input ref={inputRef} type="file" accept=".pdf,application/pdf" className="hidden" onChange={onInputChange} />
      </div>

      {error && (
        <p role="alert" className="text-sm text-red-500">{error}</p>
      )}
    </div>
  );
}
