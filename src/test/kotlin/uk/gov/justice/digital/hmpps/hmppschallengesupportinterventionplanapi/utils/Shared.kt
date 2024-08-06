package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import java.time.LocalDate

const val LOG_CODE = "ZXY987"

fun createContributoryFactorRequest(type: String = "BAS", comment: String? = "comment about the factor") =
  CreateContributoryFactorRequest(type, comment)

fun createInterviewRequest(
  roleCode: String = "OTHER",
  date: LocalDate = LocalDate.now(),
  interviewee: String = "A Person",
  notes: String? = null,
) = CreateInterviewRequest(interviewee, date, roleCode, notes)

fun createIdentifiedNeedRequest(
  identifiedNeed: String = "An identified need",
  needIdentifiedBy: String = "I Dent",
  createdDate: LocalDate = LocalDate.now(),
  targetDate: LocalDate = LocalDate.now().plusWeeks(8),
  closedDate: LocalDate? = null,
  intervention: String = "intervention description",
  progression: String? = null,
) = CreateIdentifiedNeedRequest(
  identifiedNeed,
  needIdentifiedBy,
  createdDate,
  targetDate,
  closedDate,
  intervention,
  progression,
)

fun createAttendeeRequest(
  name: String? = "name",
  role: String? = "role",
  isAttended: Boolean? = true,
  contribution: String? = "a small contribution",
) = CreateAttendeeRequest(name, role, isAttended, contribution)
