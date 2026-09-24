DROP TABLE IF EXISTS case_note_annotations;

CREATE TABLE case_note_annotations
(
    id                     UUID PRIMARY KEY NOT NULL,
    case_notes_analysed_id UUID NOT NULL,
    request_id             UUID NOT NULL,
    investigation_id       UUID NOT NULL,
    case_note_id           UUID NOT NULL,
    behaviour_type         VARCHAR(30),
    annotated_text         TEXT,
    created_date           TIMESTAMP,
    CONSTRAINT fk_case_note_annotations_case_notes_analysed
        FOREIGN KEY (case_notes_analysed_id) REFERENCES case_notes_analysed (id)
);

CREATE INDEX IF NOT EXISTS idx_case_note_annotations_case_notes_analysed_id
    ON case_note_annotations (case_notes_analysed_id);
CREATE INDEX IF NOT EXISTS idx_case_note_annotations_investigation_id
    ON case_note_annotations (investigation_id);
CREATE INDEX IF NOT EXISTS idx_case_note_annotations_case_note_id
    ON case_note_annotations (case_note_id);
CREATE INDEX IF NOT EXISTS idx_case_note_annotations_request_id
    ON case_note_annotations (request_id);

COMMENT ON TABLE case_note_annotations IS 'Evidence spans extracted from a case note during analysis. Each row is a single annotated fragment linked back to the parent analysis record for the case note.';
COMMENT ON COLUMN case_note_annotations.id IS 'Primary key. Surrogate UUID with no business meaning. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.case_notes_analysed_id IS 'Parent record linking this annotation to the case-note analysis metadata. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.request_id IS 'Identifier of the latest request that produced or updated this fragment. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.investigation_id IS 'The CSIP investigation the annotation belongs to. Supports fast lookup of all analysis evidence for a case note within one investigation. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.case_note_id IS 'Identifier of the source case note in the case notes service. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.behaviour_type IS 'What the model inferred the annotated text evidences about the person. One of RISKS_AND_TRIGGERS, USUAL_BEHAVIOUR_PRESENTATION, PROTECTIVE_FACTORS. This is a machine generated hypothesis offered to a caseworker, not a professional assessment or a recorded observation. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN case_note_annotations.annotated_text IS 'Verbatim extract copied from the source case note - the passage the model cites as evidence for behaviour_type. This can contain health, safeguarding, or other sensitive details from the original case note. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN case_note_annotations.created_date IS 'When the annotation was created or last updated. This is not the source case note date. [Sensitivity: NONE]';
