INSERT INTO storage_places (name, description, sort_order, active)
VALUES
    ('Fridge', 'Cold storage', 10, TRUE),
    ('Freezer', 'Frozen goods', 20, TRUE),
    ('Pantry', 'Dry goods', 30, TRUE);

INSERT INTO units (code, name, short_name, unit_type, active)
VALUES
    ('piece', 'Piece', 'pcs', 'COUNT', TRUE),
    ('kg', 'Kilogram', 'kg', 'WEIGHT', TRUE),
    ('g', 'Gram', 'g', 'WEIGHT', TRUE),
    ('l', 'Liter', 'l', 'VOLUME', TRUE),
    ('ml', 'Milliliter', 'ml', 'VOLUME', TRUE),
    ('pack', 'Pack', 'pack', 'PACKAGING', TRUE);

INSERT INTO categories (name, description, sort_order, active)
VALUES
    ('Food', 'Food and beverages', 10, TRUE),
    ('Household', 'Cleaning and utility', 20, TRUE),
    ('Hygiene', 'Personal care', 30, TRUE);

INSERT INTO stores (name, description, sort_order, active)
VALUES
    ('Supermarket', 'Main supermarket', 10, TRUE),
    ('Pharmacy', 'Health and hygiene store', 20, TRUE);
