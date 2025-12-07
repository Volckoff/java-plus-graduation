DROP TABLE IF EXISTS requests CASCADE;

CREATE TABLE IF NOT EXISTS requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created TIMESTAMP   NOT NULL,
    status VARCHAR(20) NOT NULL,
    requester_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT uq_requester_event UNIQUE (requester_id, event_id)
);




