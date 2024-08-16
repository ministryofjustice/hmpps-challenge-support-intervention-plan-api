package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.audit

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.envers.EntityTrackingRevisionListener
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.hibernate.envers.RevisionType
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Companion.fromClass
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.PersistenceAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

@Entity
@Table
@RevisionEntity(AuditRevisionEntityListener::class)
@EntityListeners(CsipEventEntityListener::class)
class AuditRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  var id: Long = 0

  // must be called timestamp for EnversRevisionRepositoryImpl
  @RevisionTimestamp
  var timestamp: LocalDateTime = LocalDateTime.now()

  var username: String? = null
  var userDisplayName: String? = null
  var caseloadId: String? = null

  @Enumerated(EnumType.STRING)
  var source: Source? = null

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var affectedComponents: MutableSet<CsipComponent> = mutableSetOf()

  @Transient
  var eventInformation: EventInformation = EventInformation()
}

class AuditRevisionEntityListener : EntityTrackingRevisionListener {
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
    entityClass: Class<*>,
    entityName: String,
    entityId: Any,
    revisionType: RevisionType,
    revision: Any,
  ) {
    (revision as AuditRevision).apply {
      val component = fromClass(entityClass) ?: throw IllegalArgumentException("Unknown entity type affected")
      affectedComponents.add(component)
      eventInformation.add(
        id,
        when (revisionType) {
          RevisionType.ADD -> PersistenceAction.CREATED
          RevisionType.MOD -> PersistenceAction.UPDATED
          RevisionType.DEL -> PersistenceAction.DELETED
        },
        component,
        entityId,
      )
    }
  }
}

interface AuditRevisionRepository : JpaRepository<AuditRevision, Long>