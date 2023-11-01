CREATE TABLE IF NOT EXISTS url_repository (
    id BIGSERIAL PRIMARY KEY,
    shortUrl VARCHAR(10) UNIQUE NOT NULL,
    fullUrl TEXT NOT NULL,
    creationDate TEXT NOT NULL,
    authorId INTEGER NOT NULL,
    FOREIGN KEY (authorId) REFERENCES user_registry(id)
)