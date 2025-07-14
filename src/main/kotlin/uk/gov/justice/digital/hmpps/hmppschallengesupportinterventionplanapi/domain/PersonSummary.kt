package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import java.util.Collections
import java.util.stream.Stream
import kotlin.reflect.KProperty

@Entity
@Table
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
  @field:Transient
  private var changes: MutableSet<String>? = null
  fun changes(): Set<String> = Collections.unmodifiableSet(changes ?: emptySet())
  private fun addChange(field: KProperty<*>) {
    if (changes == null) {
      changes = mutableSetOf()
    }
    changes!! += field.name
  }

  var firstName: String = firstName
    private set(value) {
      if (value != field) {
        addChange(::firstName)
      }
      field = value
    }
  var lastName: String = lastName
    private set(value) {
      if (value != field) {
        addChange(::lastName)
      }
      field = value
    }
  var status: String = status
    private set(value) {
      if (value != field) {
        addChange(::status)
      }
      field = value
    }
  var restrictedPatient: Boolean = restrictedPatient
    private set(value) {
      if (value != field) {
        addChange(::restrictedPatient)
      }
      field = value
    }
  var prisonCode: String? = prisonCode
    private set(value) {
      if (value != field) {
        addChange(::prisonCode)
      }
      field = value
    }
  var cellLocation: String? = cellLocation
    private set(value) {
      if (value != field) {
        addChange(::cellLocation)
      }
      field = value
    }
  var supportingPrisonCode: String? = supportingPrisonCode
    private set(value) {
      if (value != field) {
        addChange(::supportingPrisonCode)
      }
      field = value
    }

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
    if (changes == null) changes = mutableSetOf()
    this.firstName = firstName
    this.lastName = lastName
    this.status = status
    this.restrictedPatient = restrictedPatient
    this.prisonCode = prisonCode
    this.cellLocation = cellLocation
    this.supportingPrisonCode = supportingPrisonCode
  }
}

fun PrisonerDetails.toPersonSummary() = PersonSummary(
  prisonerNumber,
  firstName,
  lastName,
  status,
  restrictedPatient,
  prisonId,
  cellLocation,
  supportingPrisonId,
)

interface PersonSummaryRepository : JpaRepository<PersonSummary, String> {
  @Query("select p from PersonSummary p")
  fun streamAll(): Stream<PersonSummary>
  fun findAllByPrisonNumberIn(ids: Set<String>): List<PersonSummary>
}
