CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id),
    name VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL' CHECK (currency = 'BRL'),
    balance NUMERIC(17,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status VARCHAR(8) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX accounts_owner_idx ON accounts(owner_id, created_at, id);

CREATE TABLE operations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id),
    idempotency_key VARCHAR(100) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
    source_account_id UUID REFERENCES accounts(id),
    target_account_id UUID REFERENCES accounts(id),
    amount NUMERIC(17,2) NOT NULL CHECK (amount > 0),
    category VARCHAR(20) NOT NULL CHECK (category IN ('FOOD','HOUSING','TRANSPORT','HEALTH','EDUCATION','ENTERTAINMENT','SALARY','OTHER')),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT operations_owner_key_unique UNIQUE (owner_id, idempotency_key),
    CONSTRAINT operations_shape CHECK (
        (type = 'DEPOSIT' AND source_account_id IS NULL AND target_account_id IS NOT NULL) OR
        (type = 'WITHDRAWAL' AND source_account_id IS NOT NULL AND target_account_id IS NULL) OR
        (type = 'TRANSFER' AND source_account_id IS NOT NULL AND target_account_id IS NOT NULL AND source_account_id <> target_account_id)
    )
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES operations(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount NUMERIC(17,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (operation_id, account_id)
);
CREATE INDEX ledger_account_period_idx ON ledger_entries(account_id, created_at DESC, id DESC);

CREATE FUNCTION reject_financial_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Financial records are append-only' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER immutable_operations BEFORE UPDATE OR DELETE ON operations
FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();
CREATE TRIGGER immutable_ledger BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();

-- A committed operation must have exactly its expected postings, even if a future
-- application code path forgets one. Deferred checks allow atomic multi-row inserts.
CREATE FUNCTION verify_operation_postings() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    op operations%ROWTYPE;
    actual_count INTEGER;
    expected_count INTEGER;
BEGIN
    IF TG_TABLE_NAME = 'operations' THEN
        SELECT * INTO op FROM operations WHERE id = NEW.id;
    ELSE
        SELECT * INTO op FROM operations WHERE id = NEW.operation_id;
    END IF;
    expected_count := CASE WHEN op.type = 'TRANSFER' THEN 2 ELSE 1 END;
    SELECT count(*) INTO actual_count FROM ledger_entries WHERE operation_id = op.id;
    IF actual_count <> expected_count OR EXISTS (
        SELECT 1 FROM ledger_entries e WHERE e.operation_id = op.id AND
        (e.amount <> op.amount OR e.created_at <> op.created_at OR NOT (
            (e.direction = 'DEBIT' AND e.account_id = op.source_account_id) IS TRUE OR
            (e.direction = 'CREDIT' AND e.account_id = op.target_account_id) IS TRUE
        ))
    ) THEN
        RAISE EXCEPTION 'Operation postings do not balance' USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER verify_operation AFTER INSERT ON operations
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verify_operation_postings();
CREATE CONSTRAINT TRIGGER verify_posting AFTER INSERT ON ledger_entries
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verify_operation_postings();
