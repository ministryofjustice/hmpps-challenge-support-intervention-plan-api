package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData as ReferenceDataModel

fun ReferenceData.toReferenceDataModel() = ReferenceDataModel(
  code = code,
  description = description,
  listSequence = listSequence,
  deactivatedAt = deactivatedAt,
)
