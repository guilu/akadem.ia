-- RAG: source documents, chunks and generated question drafts

create table if not exists source_documents (
    id uuid primary key default gen_random_uuid(),
    name varchar(500) not null,
    type varchar(50) not null,
    version varchar(100),
    checksum varchar(64) not null,
    uploaded_at timestamp not null default now(),
    status varchar(50) not null default 'UPLOADED',
    storage_path text not null,
    constraint chk_source_documents_status
        check (status in ('UPLOADED', 'PROCESSED', 'FAILED'))
);

-- Chunks store embedding as JSON float array (text) for V1.
-- Upgrade path: ALTER TABLE source_chunks ADD COLUMN embedding_vec vector(1536)
-- then backfill and switch VectorSearchPort adapter.
create table if not exists source_chunks (
    id uuid primary key default gen_random_uuid(),
    source_document_id uuid not null,
    content text not null,
    chunk_index integer not null default 0,
    metadata text,
    embedding text,
    constraint fk_source_chunks_document
        foreign key (source_document_id) references source_documents(id)
        on delete cascade
);

create index if not exists idx_source_chunks_document_id
    on source_chunks (source_document_id);

create table if not exists generated_question_drafts (
    id uuid primary key default gen_random_uuid(),
    source_document_id uuid not null,
    unit_id uuid,
    topic varchar(500),
    difficulty varchar(50),
    statement text not null,
    answers text not null,
    correct_index integer not null,
    hint text,
    explanation text,
    reference varchar(500),
    created_at timestamp not null default now(),
    status varchar(50) not null default 'GENERATED',
    constraint fk_generated_drafts_document
        foreign key (source_document_id) references source_documents(id)
        on delete cascade,
    constraint chk_generated_drafts_status
        check (status in ('GENERATED', 'VALIDATED', 'REJECTED')),
    constraint chk_generated_drafts_correct_index
        check (correct_index >= 0 and correct_index <= 3)
);

create index if not exists idx_generated_drafts_document_id
    on generated_question_drafts (source_document_id);

create index if not exists idx_generated_drafts_unit_id
    on generated_question_drafts (unit_id);
