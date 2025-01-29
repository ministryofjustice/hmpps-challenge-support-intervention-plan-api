package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Autowired
  private lateinit var context: ApplicationContext

  @Bean
  fun customOpenAPI(): OpenAPI? = OpenAPI()
    .info(
      Info()
        .title("Challenge, Support, and Intervention Plan API")
        .version(version)
        .description(
          "API for retrieving and managing Challenge, Support, and Intervention Plan (CSIP) relating to a person.\n\n" +
            "This API is only intended for use by the CSIP UI and the prisoner profile. " +
            "Any other proposed consumers of this API should discuss the requirements first with the current maintainers of the CSIP service. " +
            "See the developer portal for this information.\n\n" +
            "## Authentication\n\n" +
            "This API uses OAuth2 with JWTs. " +
            "You will need to pass the JWT in the `Authorization` header using the `Bearer` scheme.\n\n" +
            "## Authorisation\n\n" +
            "The API uses roles to control access to the endpoints. " +
            "The roles required for each endpoint are documented in the endpoint descriptions.\n\n" +
            "## Identifying the user\n\n" +
            "The majority of the endpoints in this API require the user to be identified via their username. " +
            "This is to correctly populate the change history of CSIP records e.g. who created or updated a CSIP record and for auditing purposes. " +
            "The username is required when the service is called directly by a user or when another service is acting on behalf of a user. " +
            "The method to supply the username is via the `subject` claim in the JWT.\n" +
            "### 4XX response codes related to username:\n\n" +
            "- A 400 Bad Request response will be returned if the username cannot be found in the user management service.\n",
        )
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))
    .also {
      PrimitiveType.enablePartialTime() // Prevents generation of a LocalTime schema which causes conflicts with java.time.LocalTime
    }

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer = OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
    handlerMethod.preAuthorizeForMethodOrClass()?.let {
      val preAuthExp = SpelExpressionParser().parseExpression(it)
      val evalContext = StandardEvaluationContext()
      evalContext.beanResolver = BeanFactoryResolver(context)
      evalContext.setRootObject(
        object {
          fun hasRole(role: String) = listOf(role)
          fun hasAnyRole(vararg roles: String) = roles.toList()
        },
      )

      val roles = try {
        (preAuthExp.getValue(evalContext) as List<*>).filterIsInstance<String>()
      } catch (e: SpelEvaluationException) {
        emptyList()
      }
      if (roles.isNotEmpty()) {
        operation.description = "${operation.description ?: ""}\n\n" +
          "Requires one of the following roles:\n" +
          roles.joinToString(prefix = "* ", separator = "\n* ")
      }
    }

    operation
  }

  private fun HandlerMethod.preAuthorizeForMethodOrClass() = getMethodAnnotation(PreAuthorize::class.java)?.value
    ?: beanType.getAnnotation(PreAuthorize::class.java)?.value
}
