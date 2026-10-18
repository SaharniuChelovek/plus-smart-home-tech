CREATE TABLE IF NOT EXISTS inventory (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT  NOT NULL,
    quantity          INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version           BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT uq_inventory_product_id UNIQUE (product_id),
    CONSTRAINT chk_inventory_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_le_quantity CHECK (reserved_quantity <= quantity)
);