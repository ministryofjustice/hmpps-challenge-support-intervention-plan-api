CREATE TABLE case_notes_analysed
(
    id                               UUID PRIMARY KEY NOT NULL,
    request_id                       UUID,
    investigation_id                 UUID NOT NULL,
    prisoner_number                  VARCHAR(10) NOT NULL,
    case_note_id                     UUID,
    prompt_key                       VARCHAR(255),
    prompt_version                   INT,
    behaviour_type                   VARCHAR(30) NOT NULL,
    usual_behaviour_relevancy        INT NOT NULL DEFAULT 0,
    risks_and_triggers_relevancy     INT NOT NULL DEFAULT 0,
    protective_factors_relevancy     INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_notes_analysed_investigation_id ON case_notes_analysed(investigation_id);
CREATE INDEX idx_case_notes_analysed_prisoner_number ON case_notes_analysed(prisoner_number);
CREATE INDEX idx_case_notes_analysed_case_note_id ON case_notes_analysed(case_note_id);
