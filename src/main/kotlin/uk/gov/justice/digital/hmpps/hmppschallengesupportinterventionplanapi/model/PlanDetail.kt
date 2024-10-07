package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FirstReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.IdentifiedNeedsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReviewsRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PlanRequestValidator::class])
annotation class ValidPlanDetail(
  val message: String = "At least one of caseManager, reasonForPlan, firstCaseReviewDate, must be non null or at least one child record should be provided (identified need or review)",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class PlanRequestValidator : ConstraintValidator<ValidPlanDetail, PlanRequest> {
  override fun isValid(request: PlanRequest, context: ConstraintValidatorContext): Boolean {
    return with(request) {
      listOfNotNull(
        caseManager,
        reasonForPlan,
        if (request is FirstReviewRequest) request.firstCaseReviewDate else null,
        nextCaseReviewDate,
      ).isNotEmpty() ||
        request is IdentifiedNeedsRequest && request.identifiedNeeds.isNotEmpty() ||
        request is ReviewsRequest && request.reviews.isNotEmpty()
    }
  }
}
