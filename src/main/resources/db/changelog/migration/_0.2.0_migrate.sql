-- liquibase formatted sql

-- changeset yterinc:2

ALTER TABLE tasks
    ADD COLUMN deadline TIMESTAMP;

CREATE TABLE users
(
    chat_id   BIGINT PRIMARY KEY,
    time_zone VARCHAR(32) NOT NULL DEFAULT 'UTC'
);