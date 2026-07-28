CREATE TABLE case_note_annotations
(
    id                      UUID PRIMARY KEY NOT NULL,
    request_id              UUID,
    prisoner_number         VARCHAR(10) NOT NULL,
    case_note_id            UUID,
    prompt_key              VARCHAR(255),
    prompt_version          INT,
    behaviour_type          VARCHAR(30),
    confidence_level        VARCHAR(10),
    annotated_text          TEXT,
    created_date            TIMESTAMP
);

CREATE INDEX idx_case_note_annotations_request_id ON case_note_annotations(request_id);
CREATE INDEX idx_case_note_annotations_prisoner_number ON case_note_annotations(prisoner_number);
CREATE INDEX idx_case_note_annotations_case_note_id ON case_note_annotations(case_note_id);
