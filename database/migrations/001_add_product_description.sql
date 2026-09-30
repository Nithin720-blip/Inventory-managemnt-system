USE inventory_reservation_engine;

-- Run once when upgrading a products table created before description was added.
ALTER TABLE products
    ADD COLUMN description TEXT NULL AFTER name;
