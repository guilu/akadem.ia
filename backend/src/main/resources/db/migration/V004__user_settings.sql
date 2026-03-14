create table if not exists user_settings (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null unique,
    new_cards_limit int  not null,
    review_cards_limit int not null,
    created_at      timestamp not null default now(),
    updated_at      timestamp not null default now(),

    constraint fk_user_settings_user
        foreign key (user_id) references users(id)
        on delete cascade
);
