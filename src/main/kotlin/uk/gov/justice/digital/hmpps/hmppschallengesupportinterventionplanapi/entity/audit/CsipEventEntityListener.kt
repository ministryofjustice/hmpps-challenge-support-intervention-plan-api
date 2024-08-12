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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.SaferCustodyScreeningOutcome
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
        Record,
        Referral,
        SaferCustodyScreeningOutcome,
        Investigation,
        DecisionAndActions,
        Plan,
      )
    }

    val csipChange = csip.firstOrNull { it.component == Record } ?: csip.firstOrNull()
    csipChange?.also { changed ->
      val action: PersistenceAction = if (changed.component == Record) changed.action else PersistenceAction.UPDATED
      entityManager.find(CsipRecord::class.java, changed.id)?.also { csip ->
        add(csipEvent(csip.prisonNumber, action, csip.uuid, changes.map { it.component }.toSet()))
      }
    }

    addAll(
      children.mapNotNull { changed ->
        val entity = entityManager.find(changed.component.clazz.java, changed.id)
        if (entity is Identifiable && entity is CsipAware) {
          val record = entity.csipRecord()
          csipChildEvent(record.prisonNumber, changed.component, changed.action, record.uuid, entity.uuid)
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
