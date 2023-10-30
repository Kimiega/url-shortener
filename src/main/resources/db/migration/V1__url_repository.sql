CREATE TABLE IF NOT EXISTS url_repository (
    id serial PRIMARY KEY,
    shortUrl VARCHAR(5) UNIQUE NOT NULL,
    fullUrl TEXT UNIQUE NOT NULL,
    creationDate TEXT NOT NULL,
    authorId INTEGER NOT NULL,
    FOREIGN KEY (authorId) REFERENCES user_registry(id)
)