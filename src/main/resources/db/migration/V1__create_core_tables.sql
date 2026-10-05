CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_number VARCHAR(10) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'NGN',
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    source_account_id BIGINT NULL,
    destination_account_id BIGINT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    provider_reference VARCHAR(150),
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_dest_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_tx_source ON transactions(source_account_id);
CREATE INDEX idx_tx_dest ON transactions(destination_account_id);
CREATE INDEX idx_tx_ref ON transactions(reference_number);