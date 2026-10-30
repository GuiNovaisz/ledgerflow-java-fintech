-- Planning goals do not reserve, transfer or otherwise alter account balances.
CREATE TABLE planning_goals (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id),
    name VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    target_amount NUMERIC(17,2) NOT NULL CHECK (target_amount > 0),
    current_amount NUMERIC(17,2) NOT NULL CHECK (current_amount >= 0),
    due_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT goal_date_range CHECK (due_date IS NULL OR due_date BETWEEN DATE '1970-01-01' AND DATE '9999-12-31')
);
CREATE INDEX planning_goals_owner_idx ON planning_goals(owner_id, created_at, id);
