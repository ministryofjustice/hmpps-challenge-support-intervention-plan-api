# CSIP API Design

## Basic CSIP flow

1) Referral - Make a CSIP referral
2) Screening - Screen a CSIP referral. Safer Custody Screening. Can decide not to continue the CSIP process
3) Investigation - Record an Investigation
4) Decision - Decide outcome. Can decide not to continue the CSIP process
5) Plan - Open a CSIP
6) Review - Review the CSIP
7) Close - Close the CSIP

## Controllers and endpoints

### CSIP record controller

Contains primary retrieval endpoints

- GET /prisoner/{prisonNumber}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoner. Supports log number filtering. Returns CSIP record summaries
- GET /prison/{prisonCode}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoners resident in the prison. Supports log number filtering. Returns CSIP record summaries
- GET /csip-record/{recordUuid} - Retrieve a complete CSIP record by its unique id
- PATCH /csip-record/{recordUuid}/log-number - Update the log number for a CSIP record. Publishes prisoner-csip.csip-record-updated event with recordAffected = true
- DELETE /csip-record/{recordUuid} - (Soft) delete a complete CSIP record. Requires admin permissions. Publishes prisoner-csip.csip-record-deleted event

### Referral controller
- POST /prisoner/{prisonNumber}/csip-record/referral - Create the CSIP record, referral and contributory factors. This starts the CSIP process. Publishes prisoner-csip.csip-record-created and prisoner-csip.contributory-factor-created events
- PATCH /csip-record/{recordUuid}/referral - Update the CSIP referral only. Cannot update contributory factors with this endpoint. Publishes prisoner-csip.csip-record-created event with referralAffected = true
- POST /csip-record/{recordUuid}/referral/contributory-factors - Add a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-created event
- PATCH /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Update a contributory factor on the referral. Publishes prisoner-csip.contributory-factor-updated event
- DELETE /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Remove a contributory factor from the referral. Publishes prisoner-csip.contributory-factor-deleted event

### Screening controller
- POST /csip-record/{recordUuid}/referral/safer-custody-screening - Create the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true
- PATCH /csip-record/{recordUuid}/referral/safer-custody-screening - Update the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true

### Investigation controller
- POST /csip-record/{recordUuid}/referral/investigation - Create the investigation and any interviews. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true and prisoner-csip.interview-created event
- PATCH /csip-record/{recordUuid}/referral/investigation - Update the investigation only. Cannot update interviews with this endpoint. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true
- POST /csip-record/{recordUuid}/referral/investigation/interviews - Add an interview to the investigation. Publishes prisoner-csip.interview-created event
- PATCH /csip-record/referral/investigation/interview/{interviewUuid} - Update an interview on the investigation. Publishes prisoner-csip.interview-updated event
- DELETE /csip-record/referral/investigation/interview/{interviewUuid} - Remove an interview from the investigation. Publishes prisoner-csip.interview-deleted event

### Decision and actions controller
- POST /csip-record/{recordUuid}/referral/decision-and-actions - Create the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true
- PATCH /csip-record/{recordUuid}/referral/decision-and-actions - Update the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true

### Plan controller
- POST /csip-record/{recordUuid}/plan - Create the CSIP plan and identified needs. Publishes prisoner-csip.csip-record-updated event with planAffected = true and prisoner-csip.identified-need-created event
- PATCH /csip-record/{recordUuid}/plan - Update the plan only. Cannot update identified needs with this endpoint. Publishes prisoner-csip.csip-record-updated event with planAffected = true
- POST /csip-record/{recordUuid}/plan/identified-needs - Add an identified need to the plan. Publishes prisoner-csip.identified-need-created event
- PATCH /csip-record/plan/identified-need/{identifiedNeedUuid} - Update an identified need on the plan. Publishes prisoner-csip.identified-need-updated event
- DELETE /csip-record/plan/identified-need/{identifiedNeedUuid} - Remove an identified need from the plan. Publishes prisoner-csip.identified-need-deleted event

### Review controller
- POST /csip-record/{recordUuid}/plan/reviews - Create a review of the plan and any attendees. Publishes prisoner-csip.review-created and prisoner-csip.attendee-created events
- PATCH /csip-record/plan/review/{reviewUuid} - Update the review of the plan only. Cannot update attendees with this endpoint. Publishes prisoner-csip.review-updated event
- POST /csip-record/plan/review/{reviewUuid}/attendees - Add an attendee to the review of the plan. Publishes prisoner-csip.attendee-created event
- PATCH /csip-record/plan/review/attendee/{attendeeUuid} - Update an attendee on the review of the plan. Publishes prisoner-csip.attendee-updated event
- DELETE /csip-record/plan/review/attendee/{attendeeUuid} - Remove an attendee from the review of the plan. Publishes prisoner-csip.attendee-deleted event