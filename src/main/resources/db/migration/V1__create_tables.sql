-- 著者
CREATE TABLE authors (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    birth_date DATE        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now()
);

-- 書籍
CREATE TABLE books (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    price              INTEGER      NOT NULL,
    publication_status VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_books_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_books_publication_status CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED'))
);

-- 書籍と著者の多対多
CREATE TABLE book_authors (
    book_id   BIGINT NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES authors (id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

-- 著者に紐づく書籍検索を高速化
CREATE INDEX idx_book_authors_author_id ON book_authors (author_id);
