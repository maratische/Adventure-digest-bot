CREATE TABLE IF NOT EXISTS users
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255),
    firstname   VARCHAR(255),
    telegram_id bigint UNIQUE,
    role        VARCHAR(255) NOT NULL,
    chat_id     bigint
);

CREATE TABLE IF NOT EXISTS posts
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id bigint,
    user_id    bigint,
    content    CLOB,
    date       long,
    created    long,
    updated    long,
    status     VARCHAR(50) NOT NULL
);
