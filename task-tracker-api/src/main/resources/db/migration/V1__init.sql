CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);


CREATE TABLE tasks
(
    id          UUID PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    status      VARCHAR(50) NOT NULL,
    priority    VARCHAR(50) NOT NULL,

    creator_id  UUID NOT NULL,
    assignee_id UUID,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_task_creator
        FOREIGN KEY (creator_id)
            REFERENCES users(id),

    CONSTRAINT fk_task_assignee
        FOREIGN KEY (assignee_id)
            REFERENCES users(id)
);