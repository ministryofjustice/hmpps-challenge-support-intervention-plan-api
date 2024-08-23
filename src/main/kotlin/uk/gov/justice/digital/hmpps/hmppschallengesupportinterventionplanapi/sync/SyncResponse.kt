package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import java.util.UUID

data class SyncResponse(val mappings: Set<ResponseMapping>)

data class ResponseMapping(val component: CsipComponent, val id: Long, val uuid: UUID)
