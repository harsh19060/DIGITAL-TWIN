BEGIN;

ALTER TABLE campus_messages
    DROP CONSTRAINT IF EXISTS campus_messages_sender_type_check;
ALTER TABLE campus_messages
    ADD CONSTRAINT campus_messages_sender_type_check
    CHECK (sender_type IN ('TEACHER', 'STUDENT', 'SECTION', 'SYSTEM'));

ALTER TABLE campus_messages
    DROP CONSTRAINT IF EXISTS campus_messages_recipient_type_check;
ALTER TABLE campus_messages
    ADD CONSTRAINT campus_messages_recipient_type_check
    CHECK (recipient_type IS NULL OR recipient_type IN ('TEACHER', 'STUDENT', 'SECTION', 'SYSTEM'));

COMMIT;