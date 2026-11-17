-- Dedicated icons for the four categories that used the generic "more" icon.
UPDATE categories SET icon_key = 'stationery' WHERE slug = 'stationery'         AND icon_key = 'more';
UPDATE categories SET icon_key = 'sports'     WHERE slug = 'sports-equipment'   AND icon_key = 'more';
UPDATE categories SET icon_key = 'household'  WHERE slug = 'household'          AND icon_key = 'more';
UPDATE categories SET icon_key = 'reusable'   WHERE slug = 'reusable-materials' AND icon_key = 'more';

-- Optional map pin for the pickup spot (WGS84). Both set or both null.
ALTER TABLE items
    ADD COLUMN pickup_latitude  DECIMAL(9,6) NULL AFTER pickup_location,
    ADD COLUMN pickup_longitude DECIMAL(9,6) NULL AFTER pickup_latitude,
    ADD CONSTRAINT ck_items_pickup_coords CHECK (
        (pickup_latitude IS NULL AND pickup_longitude IS NULL)
        OR (pickup_latitude BETWEEN -90 AND 90 AND pickup_longitude BETWEEN -180 AND 180));
