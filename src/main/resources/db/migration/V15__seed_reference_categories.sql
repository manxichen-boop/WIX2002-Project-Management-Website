-- Reference data (not sample data): the initial category list. Admins can edit,
-- deactivate or add categories afterwards from Admin > Categories.
INSERT INTO categories (category_name, slug, description, icon_key, display_order) VALUES
    ('Books',              'books',              'Textbooks, references, novels and study guides',          'books',       10),
    ('Electronics',        'electronics',        'Calculators, gadgets, chargers and small electronics',    'electronics', 20),
    ('Clothing',           'clothing',           'Clothes, shoes and wearable items',                       'clothing',    30),
    ('Accessories',        'accessories',        'Backpacks, bags and everyday accessories',                'accessories', 40),
    ('Furniture',          'furniture',          'Chairs, desks, shelves and room furniture',               'furniture',   50),
    ('Stationery',         'stationery',         'Notebooks, art supplies and study tools',                 'more',        60),
    ('Sports Equipment',   'sports-equipment',   'Rackets, balls, mats and fitness gear',                   'more',        70),
    ('Household',          'household',          'Kitchenware, lamps and things for your room',             'more',        80),
    ('Reusable Materials', 'reusable-materials', 'Cardboard, glass, fabric and packaging for upcycling',     'more',        90);
