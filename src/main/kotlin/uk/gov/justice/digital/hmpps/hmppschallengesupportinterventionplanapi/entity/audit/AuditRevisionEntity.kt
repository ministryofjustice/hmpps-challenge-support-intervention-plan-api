package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.audit

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.envers.EntityTrackingRevisionListener
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.hibernate.envers.RevisionType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

@Entity
@Table
@RevisionEntity(AuditRevisionEntityListener::class)
class AuditRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  var id: Long = 0

  @RevisionTimestamp
  var datetime: LocalDateTime = LocalDateTime.now()

  var username: String? = null
  var userDisplayName: String? = null
  var caseloadId: String? = null

  @Enumerated(EnumType.STRING)
  var source: Source? = null

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var affectedComponents: MutableSet<AffectedComponent> = mutableSetOf()
}

class AuditRevisionEntityListener : RevisionListener, EntityTrackingRevisionListener {
  override fun newRevision(revision: Any?) {
    (revision as AuditRevision).apply {
      val context = csipRequestContext()
      username = context.username
      userDisplayName = context.userDisplayName
      caseloadId = context.activeCaseLoadId
      source = context.source
    }
  }

  override fun entityChanged(
    entityClass: Class<*>?,
    entityName: String?,
    entityId: Any?,
    revisionType: RevisionType?,
    revision: Any?,
  ) {
    (revision as AuditRevision).apply {
      val component = when (entityClass) {
        CsipRecord::class.java -> AffectedComponent.Record
        Referral::class.java -> AffectedComponent.Referral
        ContributoryFactor::class.java -> AffectedComponent.ContributoryFactor
        SaferCustodyScreeningOutcome::class.java -> AffectedComponent.SaferCustodyScreeningOutcome
        DecisionAndActions::class.java -> AffectedComponent.DecisionAndActions
        Investigation::class.java -> AffectedComponent.Investigation
        Interview::class.java -> AffectedComponent.Interview
        Plan::class.java -> AffectedComponent.Plan
        IdentifiedNeed::class.java -> AffectedComponent.IdentifiedNeed
        Review::class.java -> AffectedComponent.Review
        Attendee::class.java -> AffectedComponent.Attendee
        else -> throw IllegalArgumentException("Unknown entity type affected")
      }
      affectedComponents.add(component)
    }
  }
}
