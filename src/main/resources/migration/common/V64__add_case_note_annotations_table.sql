CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE case_note_annotations
(
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY NOT NULL,
    request_id              UUID,
    prisoner_id             VARCHAR(255),
    case_note_id            UUID,
    prompt_key              VARCHAR(255),
    prompt_version          INT,
    behaviour_type          VARCHAR(255),
    confidence_level        VARCHAR(10),
    annotated_text          TEXT,
    created_date            TIMESTAMP
);

CREATE INDEX idx_case_note_annotations_request_id ON case_note_annotations(request_id);
CREATE INDEX idx_case_note_annotations_prisoner_id ON case_note_annotations(prisoner_id);
CREATE INDEX idx_case_note_annotations_case_note_id ON case_note_annotations(case_note_id);

