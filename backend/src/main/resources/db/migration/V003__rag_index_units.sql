-- RAG: link source documents to subjects, add unit tracking on chunks

-- 1. Add subject_id to source_documents
ALTER TABLE source_documents ADD COLUMN IF NOT EXISTS subject_id UUID REFERENCES subjects(id);

-- 2. Add PENDING_REVIEW status to source_documents
ALTER TABLE source_documents DROP CONSTRAINT IF EXISTS chk_source_documents_status;
ALTER TABLE source_documents ADD CONSTRAINT chk_source_documents_status
    CHECK (status IN ('UPLOADED', 'PENDING_REVIEW', 'PROCESSED', 'FAILED'));

-- 3. Add unit tracking to source_chunks
ALTER TABLE source_chunks ADD COLUMN IF NOT EXISTS unit_name VARCHAR(500);
ALTER TABLE source_chunks ADD COLUMN IF NOT EXISTS unit_id UUID REFERENCES units(id);

CREATE INDEX IF NOT EXISTS idx_source_chunks_unit_id ON source_chunks (unit_id);
