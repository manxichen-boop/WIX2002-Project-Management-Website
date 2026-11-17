-- Item categories, managed by administrators.
-- slug, icon_key and display_order are additions to the base design: they give
-- stable URLs (/explore?category=books) and drive the homepage category cards.
CREATE TABLE categories (
    category_id     BIGINT        NOT NULL AUTO_INCREMENT,
    category_name   VARCHAR(80)   NOT NULL,
    slug            VARCHAR(80)   NOT NULL,
    description     VARCHAR(255)  NULL,
    icon_key        VARCHAR(40)   NOT NULL DEFAULT 'more',
    display_order   INT           NOT NULL DEFAULT 100,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (category_id),
    CONSTRAINT uk_categories_name UNIQUE (category_name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
