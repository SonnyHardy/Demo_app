CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role    VARCHAR(255)
);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES users (id);