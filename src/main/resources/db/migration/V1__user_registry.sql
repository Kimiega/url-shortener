CREATE TABLE IF NOT EXISTS user_registry (
    id serial PRIMARY KEY,
    login VARCHAR(255) UNIQUE NOT NULL,
    password text NOT NULL
)