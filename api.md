# CSIP API Design

## Create endpoints
- POST /prisoner/{prisonNumber}/csip-record/referral - Create the CSIP record, referral and any contributory factors. This starts the CSIP process. Publishes prisoner-csip.csip-record-created event
- POST /csip-record/{recordUuid}/referral/contributory-factor - Adds a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-created event

## Retrieve endpoints
- GET /prisoner/{prisonNumber}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoner. Supports log number filtering. Returns CSIP record summaries
- GET /prison/{prisonCode}/csip-records?filterParams=values - Retrieve and filter all CSIP records for prisoners resident in the prison. Supports log number filtering. Returns CSIP record summaries
- GET /csip-record/{recordUuid} - Retrieve a complete CSIP record by its unique id

## Update endpoints
- PUT /csip-record/referral/contributory-factor/{contributoryFactorUuid} - Updates a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-updated
- PUT /csip-record/{recordUuid}/referral/safer-custody-screening - Creates or replaces the safer custody screening entity associated with the referral

## Delete endpoints
- DELETE /csip-record/referral/contributory-factor/{contributoryFactorUuid}