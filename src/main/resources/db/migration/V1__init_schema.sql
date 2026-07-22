-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Categories (system default categories have user_id = NULL)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    user_id BIGINT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (name, type, user_id)
);

-- Groups (for shared/group expenses)
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Group membership
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);

-- Transactions (income is always personal; expense may optionally belong to a group)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    txn_date DATE NOT NULL,
    description VARCHAR(500),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by BIGINT NULL REFERENCES users(id),   -- who actually paid, for group expenses
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_income_no_group CHECK (
        (type = 'INCOME' AND group_id IS NULL) OR (type = 'EXPENSE')
    )
);

CREATE INDEX idx_transactions_user_date ON transactions(user_id, txn_date);
CREATE INDEX idx_transactions_group ON transactions(group_id);

-- Expense splits (only rows for group expenses)
CREATE TABLE expense_splits (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    share_amount NUMERIC(14, 2) NOT NULL CHECK (share_amount >= 0),
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (transaction_id, user_id)
);

-- Settlements (records of a payment made between two group members to clear balances)
CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by BIGINT NOT NULL REFERENCES users(id),
    paid_to BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    settled_at TIMESTAMP NOT NULL DEFAULT now(),
    note VARCHAR(300)
);

CREATE INDEX idx_settlements_group ON settlements(group_id);

-- Seed default categories (shared across all users, user_id IS NULL)
INSERT INTO categories (name, type, user_id) VALUES
    ('Salary', 'INCOME', NULL),
    ('Freelance', 'INCOME', NULL),
    ('Interest', 'INCOME', NULL),
    ('Gift', 'INCOME', NULL),
    ('Other Income', 'INCOME', NULL),
    ('Food', 'EXPENSE', NULL),
    ('Rent', 'EXPENSE', NULL),
    ('Travel', 'EXPENSE', NULL),
    ('Utilities', 'EXPENSE', NULL),
    ('Entertainment', 'EXPENSE', NULL),
    ('Shopping', 'EXPENSE', NULL),
    ('Health', 'EXPENSE', NULL),
    ('Other Expense', 'EXPENSE', NULL);
