package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import java.util.UUID

interface Identifiable {
  val id: UUID
  val legacyId: Long?
}

fun Identifiable.byId(uuid: UUID?): Boolean = uuid != null && id == uuid
fun Identifiable.byLegacyId(id: Long): Boolean = legacyId == id
