package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Transactional

interface RefreshRepository<T, ID> {
  fun refresh(t: T)
}

@Transactional
class RefreshRepositoryImpl<T, ID>(private val entityManager: EntityManager) : RefreshRepository<T, ID> {
  override fun refresh(t: T) {
    entityManager.refresh(t)
  }
}
