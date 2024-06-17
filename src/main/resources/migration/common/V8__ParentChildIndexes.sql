CREATE INDEX idx_referral_incident_type_id ON referral(incident_type_id);
CREATE INDEX idx_referral_incident_location_id ON referral(incident_location_id);
CREATE INDEX idx_referral_referer_area_of_work_id ON referral(referer_area_of_work_id);
CREATE INDEX idx_referral_incident_involvement_id ON referral(incident_involvement_id);
CREATE INDEX idx_referral_record_id ON referral(record_id);

CREATE INDEX idx_contributory_factor_record_id ON contributory_factor(record_id);
CREATE INDEX idx_contributory_factor_contributory_factor_type_id ON contributory_factor(contributory_factor_type_id);

CREATE INDEX idx_safer_custody_screening_outcome_outcome_id ON safer_custody_screening_outcome(outcome_id);
CREATE INDEX idx_safer_custody_screening_outcome_record_id ON safer_custody_screening_outcome(record_id);

CREATE INDEX idx_investigation_record_id ON investigation(record_id);

CREATE INDEX idx_interview_record_id ON interview(record_id);
CREATE INDEX idx_interview_interviewee_role_id ON interview(interviewee_role_id);

CREATE INDEX idx_decision_and_actions_decision_outcome_id ON decision_and_actions(decision_outcome_id);
CREATE INDEX idx_decisions_and_actions_decision_outcome_signed_off_role_id ON decision_and_actions(decision_outcome_signed_off_by_role_id);

CREATE INDEX idx_plan_record_id ON plan(record_id);

CREATE INDEX idx_identified_need_record_id ON identified_need(record_id);

CREATE INDEX idx_review_record_id ON review(record_id);

CREATE INDEX idx_attendee_review_id ON attendee(review_id);

CREATE INDEX idx_audit_event_csip_record_id ON audit_event(csip_record_id);