package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import com.fasterxml.uuid.Generators
import java.util.UUID

fun newUuid(): UUID = Generators.timeBasedEpochGenerator().generate()
