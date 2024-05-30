package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData

fun ReferenceData.toReferenceDataModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData(
    code = code,
    description = description,
    listSequence = listSequence,
    createdAt = createdAt,
    createdBy = createdBy,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    deactivatedAt = deactivatedAt,
    deactivatedBy = deactivatedBy,
  )

fun Collection<ReferenceData>.toReferenceDataModels(includeInactive: Boolean) =
  filter { includeInactive || it.isActive() }
    .sortedWith(compareBy({ it.listSequence }, { it.code }))
    .map { it.toReferenceDataModel() }