CREATE TABLE payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NULL,
    category VARCHAR(30) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    recipient_id VARCHAR(80) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    client_reference VARCHAR(100) NOT NULL UNIQUE,
    provider_reference VARCHAR(150) NULL,
    token VARCHAR(80) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_po_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_po_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_po_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_po_user ON payment_orders(user_id);
CREATE INDEX idx_po_status ON payment_orders(status);
CREATE INDEX idx_po_recipient ON payment_orders(recipient_id);