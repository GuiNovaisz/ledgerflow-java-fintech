# LedgerFlow diagrams

These diagrams describe the current implementation. See [architecture.md](architecture.md) for invariants, security boundaries, and transaction behavior.

## Request and persistence shape

```mermaid
flowchart LR
    C[HTTP client] --> F[Spring Security JWT validation]
    F --> A[auth controllers and services]
    F --> AC[account controllers and services]
    F --> L[ledger controllers and transaction service]
    F --> R[reporting controllers and services]
    A --> DB[(PostgreSQL 16)]
    AC --> DB
    L -->|one transaction: lock, balance, operation, postings| DB
    R -->|owner-scoped reads| DB
```

## Entity relationship diagram

```mermaid
erDiagram
    APP_USERS ||--o{ ACCOUNTS : owns
    APP_USERS ||--o{ OPERATIONS : initiates
    ACCOUNTS ||--o{ LEDGER_ENTRIES : receives
    OPERATIONS ||--|{ LEDGER_ENTRIES : records
    ACCOUNTS o|--o{ OPERATIONS : source
    ACCOUNTS o|--o{ OPERATIONS : target
    APP_USERS {
        uuid id PK
        string email UK
        string password_hash
        string display_name
        timestamptz created_at
    }
    ACCOUNTS {
        uuid id PK
        uuid owner_id FK
        string name
        string currency
        numeric balance
        string status
        bigint version
        timestamptz created_at
    }
    OPERATIONS {
        uuid id PK
        uuid owner_id FK
        string idempotency_key
        string fingerprint
        string type
        uuid source_account_id FK
        uuid target_account_id FK
        numeric amount
        string category
        string description
        timestamptz created_at
    }
    LEDGER_ENTRIES {
        uuid id PK
        uuid operation_id FK
        uuid account_id FK
        string direction
        numeric amount
        timestamptz created_at
    }
```

`source_account_id` and `target_account_id` are nullable because deposit and withdrawal have only one endpoint. The pair `(owner_id, idempotency_key)` has a composite unique constraint. A transfer has both endpoints and produces one `DEBIT` and one `CREDIT` posting. Operations and postings are append-only; the account balance is a locked, cached current balance.
