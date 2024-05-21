# CSIP API Design

## Basic CSIP flow

1) Referral - Make a CSIP referral
2) Screening - Screen a CSIP referral. Can decide not to continue the CSIP process
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
- PATCH /csip-record/log-number - Update the log number for a CSIP record. Publishes prisoner-csip.csip-record-updated event with recordAffected = true

### Referral controller
- POST /prisoner/{prisonNumber}/csip-record/referral - Create the CSIP record, referral and any contributory factors. This starts the CSIP process. Publishes prisoner-csip.csip-record-created event
- PATCH /csip-record/{recordUuid}/referral - Update the CSIP referral only. Cannot update contributory factors with this endpoint. Publishes prisoner-csip.csip-record-created event with referralAffected = true
- POST /csip-record/{recordUuid}/referral/contributory-factor - Add a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-created event
- PATCH /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Update a contributory factor on the referral. Publishes prisoner-csip.contributory-factor-updated event
- DELETE /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Remove a contributory factor from the referral. Publishes prisoner-csip.contributory-factor-deleted event

### Screening controller
- POST /csip-record/{recordUuid}/referral/safer-custody-screening - Create the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true
- PATCH /csip-record/{recordUuid}/referral/safer-custody-screening - Update the safer custody screening outcome. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true

### Investigation controller
- POST /csip-record/{recordUuid}/referral/investigation - Create the investigation. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true
- PATCH /csip-record/{recordUuid}/referral/investigation - Update the investigation. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true
- POST /csip-record/{recordUuid}/investigation/interview - Add an interview to the investigation. Publishes prisoner-csip.interview-created event
- PATCH /csip-record/investigation/interview/{interviewUuid} - Update an interview on the investigation. Publishes prisoner-csip.interview-updated event
- DELETE /csip-record/investigation/interview/{interviewUuid} - Remove an interview from the investigation. Publishes prisoner-csip.interview-deleted event

### Decision and actions controller
- POST /csip-record/{recordUuid}/referral/decision-and-actions - Create the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true
- PATCH /csip-record/{recordUuid}/referral/decision-and-actions - Update the decision and actions. Publishes prisoner-csip.csip-record-updated event with decisionAndActionsAffected = true

### Update endpoints
- PUT /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Updates a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-updated
- PUT /csip-record/{recordUuid}/referral/safer-custody-screening - Creates or replaces the safer custody screening entity associated with the referral. Publishes prisoner-csip.csip-record-updated event with saferCustodyScreeningOutcomeAffected = true

### Delete endpoints
- DELETE /csip-record/referral/contributory-factor/{contributoryFactorUuid}