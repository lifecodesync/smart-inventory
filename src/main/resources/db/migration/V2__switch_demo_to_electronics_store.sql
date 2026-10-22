-- V2: replace the first demo data set with the electronics/mobile store demo data.
-- This empties the product and stock tables. On the next start, DemoDataSeeder sees an
-- empty product table and creates 12 store products with 90 days of sales history.
-- Login users (app_user) are kept.
TRUNCATE TABLE stock_movement, product RESTART IDENTITY;
