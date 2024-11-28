package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.envers.Audited
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails

@Entity
@Table
@Audited(withModifiedFlag = true)
class PersonSummary(
  @Id
  val prisonNumber: String,
  firstName: String,
  lastName: String,
  status: String,
  restrictedPatient: Boolean,
  prisonCode: String?,
  cellLocation: String?,
  supportingPrisonCode: String?,
) {
  var firstName: String = firstName
    private set
  var lastName: String = lastName
    private set
  var status: String = status
    private set
  var restrictedPatient: Boolean = restrictedPatient
    private set
  var prisonCode: String? = prisonCode
    private set
  var cellLocation: String? = cellLocation
    private set
  var supportingPrisonCode: String? = supportingPrisonCode
    private set

  @Version
  val version: Int? = null

  fun update(
    firstName: String,
    lastName: String,
    status: String,
    restrictedPatient: Boolean,
    prisonCode: String?,
    cellLocation: String?,
    supportingPrisonCode: String?,
  ) {
    this.firstName = firstName
    this.lastName = lastName
    this.status = status
    this.restrictedPatient = restrictedPatient
    this.prisonCode = prisonCode
    this.cellLocation = cellLocation
    this.supportingPrisonCode = supportingPrisonCode
  }
}

fun PrisonerDetails.toPersonSummary() =
  PersonSummary(
    prisonerNumber,
    firstName,
    lastName,
    status,
    restrictedPatient,
    prisonId,
    cellLocation,
    supportingPrisonId,
  )

interface PersonSummaryRepository : JpaRepository<PersonSummary, String>
