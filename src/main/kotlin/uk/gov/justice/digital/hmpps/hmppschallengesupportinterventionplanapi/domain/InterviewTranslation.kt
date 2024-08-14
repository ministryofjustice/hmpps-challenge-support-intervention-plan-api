package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview as InterviewModel

fun Interview.toModel() = InterviewModel(
  interviewUuid = id,
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
