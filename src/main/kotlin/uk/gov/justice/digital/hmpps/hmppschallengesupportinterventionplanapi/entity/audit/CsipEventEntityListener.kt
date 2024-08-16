package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.audit

import jakarta.persistence.EntityManager
import jakarta.persistence.PreUpdate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Identifiable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBaseEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.EventFactory.csipChildEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.EventFactory.csipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.DECISION_AND_ACTIONS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INVESTIGATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.SAFER_CUSTODY_SCREENING_OUTCOME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.PersistenceAction

@Component
class CsipEventEntityListener {

  private lateinit var entityManager: EntityManager
  private lateinit var publisher: ApplicationEventPublisher

  @Autowired
  fun setEntityManager(@Lazy entityManager: EntityManager) {
    this.entityManager = entityManager
  }

  @Autowired
  fun setPublisher(@Lazy publisher: ApplicationEventPublisher) {
    this.publisher = publisher
  }

  @PreUpdate
  fun preUpdate(entity: AuditRevision) {
    entity.eventInformation.createEvents(entityManager).forEach(publisher::publishEvent)
  }
}

class EventInformation {
  private val changes: MutableSet<PersistedChange> = mutableSetOf()
  fun add(rev: Long, action: PersistenceAction, component: CsipComponent, id: Any) {
    changes += PersistedChange(rev, action, component, id)
  }

  fun createEvents(entityManager: EntityManager): List<CsipBaseEvent> = buildList {
    val (csip, children) = changes.partition {
      it.component in listOf(
        RECORD,
        REFERRAL,
        SAFER_CUSTODY_SCREENING_OUTCOME,
        INVESTIGATION,
        DECISION_AND_ACTIONS,
        PLAN,
      )
    }

    val csipChange = csip.firstOrNull { it.component == RECORD } ?: csip.firstOrNull()
    csipChange?.also { changed ->
      val action: PersistenceAction = if (changed.component == RECORD) changed.action else PersistenceAction.UPDATED
      entityManager.find(CsipRecord::class.java, changed.id)?.also { csip ->
        add(csipEvent(csip.prisonNumber, action, csip.id, changes.map { it.component }.toSet()))
      }
    }

    addAll(
      children.mapNotNull { changed ->
        val entity = entityManager.find(changed.component.clazz.java, changed.id)
        if (entity is Identifiable && entity is CsipAware) {
          val record = entity.csipRecord()
          csipChildEvent(record.prisonNumber, changed.component, changed.action, record.id, entity.id)
        } else {
          null
        }
      },
    )
  }

  private data class PersistedChange(
    val revision: Long,
    val action: PersistenceAction,
    val component: CsipComponent,
    val id: Any,
  )
}