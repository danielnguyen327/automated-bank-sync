create table app_user (
  id            uuid primary key default gen_random_uuid(),
  email         text not null unique,
  name          text not null,
  password_hash text, -- null for Google/GitHub accounts
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
);

create table plaid_item (
  id                     uuid primary key default gen_random_uuid(),
  user_id                uuid not null references app_user (id) on delete cascade,
  plaid_item_id          text not null unique,
  institution_id         text,
  institution_name       text not null,
  access_token_encrypted text not null, -- the plain text is never stored
  sync_cursor            text,
  status                 text not null default 'active'
                           check (status in ('active', 'login_required', 'error')),  
  last_synced_at         timestamptz,
  created_at             timestamptz not null default now(),
  updated_at             timestamptz not null default now(),
);
create index plaid_item_user_id_idx on plaid_item (user_id);

create table financial_account (
  id                      uuid primary key default gen_random_uuid(),
  user_id                 uuid not null references app_user (id) on delete cascade,
  item_id                 uuid not null references plaid_item (id) on delete cascade,
  plaid_account_id        text not null unique,
  name                    text not null,
  official_name           text,
  mask                    text,
  type                    text not null
                            check (type in ('depository', 'credit', 'loan', 'investment', 'other')),
  subtype                 text,
  current_balance_cents   bigint, -- money is stored as whole cents
  available_balance_cents bigint,
  iso_currency_code       text,
  created_at              timestamptz not null default now(),
  updated_at              timestamptz not null default now(),
);
create index financial_account_user_id_idx on financial_account (user_id);
create index financial_account_item_id_idx on financial_account (item_id);

create table bank_transaction (
  id                     uuid primary key default gen_random_uuid(),
  user_id                uuid not null references app_user (id) on delete cascade,
  account_id             uuid not null references financial_account (id) on delete cascade,
  plaid_transaction_id   text not null unique,
  pending_transaction_id text,            
  amount_cents           bigint not null,  
  iso_currency_code      text,
  transaction_date       date not null,
  authorized_date        date,
  name                   text not null,
  merchant_name          text,
  pending                boolean not null default false,
  pfc_primary            text,            
  pfc_detailed           text,
  category               text,
  category_source        text check (category_source in ('plaid', 'rules', 'claude')),
  created_at             timestamptz not null default now(),
  updated_at             timestamptz not null default now()
);
create index bank_transaction_user_date_idx on bank_transaction (user_id, transaction_date);
create index bank_transaction_account_id_idx on bank_transaction (account_id);

create table balance_snapshot (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references app_user (id) on delete cascade,
  account_id      uuid not null references financial_account (id) on delete cascade,
  snapshot_date   date not null,
  current_cents   bigint,
  available_cents bigint,
  created_at      timestamptz not null default now(),
  unique (account_id, snapshot_date)
);
create index balance_snapshot_user_id_idx on balance_snapshot (user_id);

create table sync_run (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references app_user (id) on delete cascade,
  item_id        uuid not null references plaid_item (id) on delete cascade,
  status         text not null default 'running'
                   check (status in ('running', 'succeeded', 'failed')),
  started_at     timestamptz not null default now(),
  finished_at    timestamptz,
  added_count    integer not null default 0,
  modified_count integer not null default 0,
  removed_count  integer not null default 0,
  error_code     text,
  error_message  text
);
create index sync_run_item_started_idx on sync_run (item_id, started_at);

create table insight_set (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references app_user (id) on delete cascade,
  period_start date not null,
  period_end   date not null,
  facts        jsonb not null, 
  sentences    jsonb,          
  generated_at timestamptz not null default now()
);
create index insight_set_user_generated_idx on insight_set (user_id, generated_at);
