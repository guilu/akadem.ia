create extension if not exists "pgcrypto";

create table if not exists subjects (
    id uuid primary key,
    name varchar(255) not null,
    description text
);

create table if not exists users (
    id uuid primary key,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    role varchar(50) not null,
    first_name varchar(255),
    last_name varchar(255),
    occupation varchar(255),
    constraint uq_users_email unique (email)
);

create table if not exists units (
    id uuid primary key,
    name varchar(255) not null,
    description text,
    order_index integer not null default 0,
    subject_id uuid not null,
    constraint fk_units_subject
        foreign key (subject_id) references subjects(id)
        on delete cascade
);

create table if not exists flashcards (
    id uuid primary key default gen_random_uuid(),
    unit_id uuid not null,
    front text not null,
    back text not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint fk_flashcards_unit
        foreign key (unit_id) references units(id)
        on delete cascade
);

create table if not exists flashcard_reviews (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    flashcard_id uuid not null,
    state varchar(20) not null,
    ease_factor numeric(4,2) not null,
    interval_days integer not null,
    repetitions integer not null,
    lapses integer not null,
    due_at timestamp not null,
    last_reviewed_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint fk_flashcard_reviews_user
        foreign key (user_id) references users(id)
        on delete cascade,
    constraint fk_flashcard_reviews_flashcard
        foreign key (flashcard_id) references flashcards(id)
        on delete cascade,
    constraint uq_flashcard_reviews_user_flashcard unique (user_id, flashcard_id),
    constraint chk_flashcard_reviews_state
        check (state in ('NEW', 'LEARNING', 'REVIEW'))
);

create table if not exists flashcard_review_log (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    flashcard_id uuid not null,
    grade varchar(10) not null,
    reviewed_at timestamp not null,
    interval_before integer,
    interval_after integer,
    ease_before numeric(4,2),
    ease_after numeric(4,2),
    created_at timestamp not null default now(),
    constraint fk_flashcard_review_log_user
        foreign key (user_id) references users(id)
        on delete cascade,
    constraint fk_flashcard_review_log_flashcard
        foreign key (flashcard_id) references flashcards(id)
        on delete cascade,
    constraint chk_flashcard_review_log_grade
        check (grade in ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

create table if not exists questions (
    id uuid primary key,
    unit_id uuid not null,
    text text not null,
    explanation text,
    difficulty varchar(255),
    constraint fk_questions_unit
        foreign key (unit_id) references units(id)
        on delete cascade
);

create table if not exists answers (
    id uuid primary key,
    question_id uuid not null,
    text text not null,
    correct boolean not null default false,
    constraint fk_answers_question
        foreign key (question_id) references questions(id)
        on delete cascade
);

create table if not exists exam_attempts (
    id uuid primary key,
    user_email varchar(255) not null,
    started_at timestamp,
    finished_at timestamp,
    total_time_seconds integer,
    score integer
);

create table if not exists exam_attempt_answers (
    id uuid primary key,
    attempt_id uuid not null,
    question_id uuid not null,
    selected_answer_id uuid
);

create index if not exists idx_flashcard_reviews_user_due_at
    on flashcard_reviews (user_id, due_at);

create index if not exists idx_flashcard_reviews_flashcard
    on flashcard_reviews (flashcard_id);

create index if not exists idx_flashcards_unit_id
    on flashcards (unit_id, id);

create index if not exists idx_flashcard_review_log_user_reviewed_at
    on flashcard_review_log (user_id, reviewed_at);

create index if not exists idx_flashcard_review_log_flashcard
    on flashcard_review_log (flashcard_id);
