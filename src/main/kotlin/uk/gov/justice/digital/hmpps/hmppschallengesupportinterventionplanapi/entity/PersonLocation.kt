package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.envers.Audited
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto.PrisonerDto

@Entity
@Table
@Audited(withModifiedFlag = true)
class PersonLocation(
  @Id
  val prisonNumber: String,
  firstName: String,
  lastName: String,
  status: String,
  prisonCode: String?,
  cellLocation: String?,
) {
  var firstName: String = firstName
    private set
  var lastName: String = lastName
    private set
  var status: String = status
    private set
  var prisonCode: String? = prisonCode
    private set
  var cellLocation: String? = cellLocation
    private set

  @Version
  val version: Int? = null
}

fun PrisonerDto.toPersonLocation() = PersonLocation(prisonerNumber, firstName, lastName, status, prisonId, cellLocation)

interface PersonLocationRepository : JpaRepository<PersonLocation, String>
