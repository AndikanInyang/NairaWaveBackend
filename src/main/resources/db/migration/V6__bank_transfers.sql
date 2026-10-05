ALTER TABLE payment_orders
    ADD COLUMN bank_code VARCHAR(10) NULL AFTER provider,
    ADD COLUMN destination_account_number VARCHAR(10) NULL AFTER recipient_id,
    ADD COLUMN destination_account_name VARCHAR(150) NULL AFTER destination_account_number,
    ADD COLUMN fee DECIMAL(19, 2) NULL DEFAULT 0.00 AFTER amount;

CREATE TABLE bank_transfer_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_code VARCHAR(10) NOT NULL,
    account_number VARCHAR(10) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    provider_recipient_code VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_btr_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_btr_user_bank_acct (user_id, bank_code, account_number)
);

CREATE INDEX idx_btr_user ON bank_transfer_recipients(user_id);