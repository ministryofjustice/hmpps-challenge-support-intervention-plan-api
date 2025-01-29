package uk.gov.justice.digital.hmpps.casenotes

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.feed
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import java.time.Duration.ofMinutes

class CsipSimulation : Simulation() {

  private val personIdentifiers = csv("person-identifiers-${getenv("ENVIRONMENT_NAME")}.csv").random()

  private fun getCurrentCsip() = exec(
    http("Get current csip of a person")
      .get("/prisoners/#{personIdentifier}/csip-records/current")
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private fun searchForCsip() = exec(
    http("Find first page of csip records for prison")
      .get("/search/csip-records")
      .queryParam("prisonCode", "#{prisonCode}")
      .queryParam("query", "#{personIdentifier}")
      .queryParam("size", 25)
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.totalPages").exists().saveAs("totalPages")),
  )

  private val prisonerProfile = scenario("Viewing prisoner profile").exec(getToken)
    .repeat(10).on(feed(personIdentifiers), getCurrentCsip())

  private val viewCsipForPerson = scenario("Viewing csip details for a person").exec(getToken)
    .repeat(1).on(feed(personIdentifiers), getCurrentCsip(), searchForCsip())

  private val createCsipForPerson = scenario("Creating a csip for a person").exec(getToken)
    .repeat(1).on(feed(personIdentifiers), getCurrentCsip())

  init {
    setUp(
      prisonerProfile.injectClosed(constantConcurrentUsers(40).during(ofMinutes(10))),
      viewCsipForPerson.injectOpen(atOnceUsers(2), rampUsersPerSec(0.5).to(20.0).during(ofMinutes(10))),
      createCsipForPerson.injectOpen(atOnceUsers(1), rampUsersPerSec(0.2).to(10.0).during(ofMinutes(10))),
    ).protocols(httpProtocol)
  }
}
