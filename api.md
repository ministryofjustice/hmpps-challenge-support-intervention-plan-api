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

- GET /prisoners/{prisonNumber}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoner. Supports log number filtering. Returns CSIP record summaries
- GET /prisons/{prisonCode}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoners resident in the prison. Supports log number filtering. Returns CSIP record summaries
- GET /csip-records/{recordUuid} - Retrieve a complete CSIP record by its unique id
- PATCH /csip-records/{recordUuid}/log-number - Update the log number for a CSIP record. Publishes prisoner-csip.csip-record-updated event with recordAffected = true
- DELETE /csip-records/{recordUuid} - (Soft) delete a complete CSIP record. Requires admin permissions. Publishes prisoner-csip.csip-record-deleted event

### Referral controller
- POST /prisoners/{prisonNumber}/csip-records/referral - Create the CSIP record, referral and contributory factors. This starts the CSIP process. Publishes prisoner-csip.csip-record-created and prisoner-csip.contributory-factor-created events
- PATCH /csip-records/{recordUuid}/referral - Update the CSIP referral only. Cannot update contributory factors with this endpoint. Publishes prisoner-csip.csip-record-created event with referralAffected = true
- POST /csip-records/{recordUuid}/referral/contributory-factors - Add a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-created event
- PATCH /csip-records/referral/contributory-factors/{contributoryFactorUuid} - Update a contributory factor on the referral. Publishes prisoner-csip.contributory-factor-updated event
- DELETE /csip-records/referral/contributory-factors/{contributoryFactorUuid} - Remove a contributory factor from the referral. Publishes prisoner-csip.contributory-factor-deleted event

### Screening controller
- POST /csip-records/{recordUuid}/referral/safer-custody-screening - Create the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true
- PATCH /csip-records/{recordUuid}/referral/safer-custody-screening - Update the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true

### Investigation controller
- POST /csip-records/{recordUuid}/referral/investigation - Create the investigation and any interviews. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true and prisoner-csip.interview-created event
- PATCH /csip-records/{recordUuid}/referral/investigation - Update the investigation only. Cannot update interviews with this endpoint. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true
- POST /csip-records/{recordUuid}/referral/investigation/interviews - Add an interview to the investigation. Publishes prisoner-csip.interview-created event
- PATCH /csip-records/referral/investigation/interviews/{interviewUuid} - Update an interview on the investigation. Publishes prisoner-csip.interview-updated event
- DELETE /csip-records/referral/investigation/interviews/{interviewUuid} - Remove an interview from the investigation. Publishes prisoner-csip.interview-deleted event

### Decision and actions controller
- POST /csip-records/{recordUuid}/referral/decision-and-actions - Create the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true
- PATCH /csip-records/{recordUuid}/referral/decision-and-actions - Update the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true

### Plan controller
- POST /csip-records/{recordUuid}/plan - Create the CSIP plan and identified needs. Publishes prisoner-csip.csip-record-updated event with planAffected = true and prisoner-csip.identified-need-created event
- PATCH /csip-records/{recordUuid}/plan - Update the plan only. Cannot update identified needs with this endpoint. Publishes prisoner-csip.csip-record-updated event with planAffected = true
- POST /csip-records/{recordUuid}/plan/identified-needs - Add an identified need to the plan. Publishes prisoner-csip.identified-need-created event
- PATCH /csip-records/plan/identified-needs/{identifiedNeedUuid} - Update an identified need on the plan. Publishes prisoner-csip.identified-need-updated event
- DELETE /csip-records/plan/identified-needs/{identifiedNeedUuid} - Remove an identified need from the plan. Publishes prisoner-csip.identified-need-deleted event

### Review controller
- POST /csip-records/{recordUuid}/plan/reviews - Create a review of the plan and any attendees. Publishes prisoner-csip.review-created and prisoner-csip.attendee-created events
- PATCH /csip-records/plan/reviews/{reviewUuid} - Update a review of the plan only. Cannot update attendees with this endpoint. Publishes prisoner-csip.review-updated event
- POST /csip-records/plan/reviews/{reviewUuid}/attendees - Add an attendee to a review of the plan. Publishes prisoner-csip.attendee-created event
- PATCH /csip-records/plan/reviews/attendees/{attendeeUuid} - Update an attendee on a review of the plan. Publishes prisoner-csip.attendee-updated event
- DELETE /csip-records/plan/reviews/attendees/{attendeeUuid} - Remove an attendee from a review of the plan. Publishes prisoner-csip.attendee-deleted event