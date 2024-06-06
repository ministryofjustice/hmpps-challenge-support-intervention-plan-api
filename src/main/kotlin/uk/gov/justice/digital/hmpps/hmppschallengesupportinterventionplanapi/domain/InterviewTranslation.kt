package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview

fun Interview.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview(
  interviewUuid = interviewUuid,
  interviewee = interviewee,
  interviewDate = interviewDate,
  intervieweeRole = intervieweeRole.toReferenceDataModel(),
  interviewText = interviewText,
  createdAt = createdAt,
  createdBy = createdBy,
  createdByDisplayName = createdByDisplayName,
  lastModifiedAt = lastModifiedAt,
  lastModifiedBy = lastModifiedBy,
  lastModifiedByDisplayName = lastModifiedByDisplayName,
)
