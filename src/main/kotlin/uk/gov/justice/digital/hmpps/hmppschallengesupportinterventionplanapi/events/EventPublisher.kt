package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events

import jakarta.persistence.EntityManager
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import java.util.UUID

@Aspect
@Component
@Transactional(propagation = Propagation.MANDATORY)
class EventPublisher(private val em: EntityManager, private val aep: ApplicationEventPublisher) {
  private val events: ThreadLocal<MutableSet<PersonCsip>> = ThreadLocal.withInitial { mutableSetOf() }

  fun csipEvent(prisonNumber: String, recordUuid: UUID) {
    events.get().add(PersonCsip(prisonNumber, recordUuid))
  }

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent)")
  fun beforePublish() {
    events.set(mutableSetOf())
  }

  @After("@annotation(pce)")
  fun publish(pce: PublishCsipEvent) {
    em.flush()
    val context = csipRequestContext()
    events.get().forEach { aep.publishEvent(CsipEvent(pce.type, it.personIdentifier, it.csipId, context.requestAt)) }
    events.get().clear()
  }
}

private data class PersonCsip(val personIdentifier: String, val csipId: UUID)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublishCsipEvent(val type: DomainEventType)
