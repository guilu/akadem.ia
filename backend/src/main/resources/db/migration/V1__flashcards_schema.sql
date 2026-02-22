create table if not exists flashcards (
    id bigserial primary key,
    unit_id bigint not null,
    front text not null,
    back text not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint fk_flashcards_unit
        foreign key (unit_id) references units(id)
        on delete cascade
);

create table if not exists flashcard_reviews (
    id bigserial primary key,
    user_id bigint not null,
    flashcard_id bigint not null,
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
    id bigserial primary key,
    user_id bigint not null,
    flashcard_id bigint not null,
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
