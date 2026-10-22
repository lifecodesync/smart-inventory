-- Runs once when the Postgres container is first created: a separate DB for automated tests.
CREATE DATABASE inventory_test OWNER inventory;
