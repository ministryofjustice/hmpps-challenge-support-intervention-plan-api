package uk.gov.justice.digital.hmpps.casenotes

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

class PrisonAdminSimulation : Simulation() {

  private val prisonCodes = csv("prison-codes-${getenv("ENVIRONMENT_NAME")}.csv").random()

  private fun overviewByPrison() = exec(
    http("Get overview for prison #{prisonCode}")
      .get("/prisons/#{prisonCode}/csip-records/overview")
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private fun retrieveForPrison(pageSize: Int) = exec(
    http("Find first page of csip records for prison")
      .get("/search/csip-records")
      .queryParam("prisonCode", "#{prisonCode}")
      .queryParam("size", pageSize)
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.metadata.totalElements").exists().saveAs("totalElements")),
  ).exec {
    it.set("totalElements", "#{totalElements}")
    it.set("currentPage", 1)
    val elements = it.getInt("totalElements")
    it.set("totalPages", elements/pageSize)
  }.repeat { it.getString("totalPages")?.toInt()?.minus(1) ?: 0 }.on(
    exec { it.set("currentPage", it.getInt("currentPage") + 1) }
      .exec(
        http("Find csip records page #{currentPage} / #{totalPages}")
          .get("/search/csip-records")
          .queryParam("prisonCode", "#{prisonCode}")
          .queryParam("size", pageSize)
          .queryParam("page", "#{currentPage}")
          .headers(authorisationHeader)
          .check(status().shouldBe(200)),
      ),
  )

  private val overview = scenario("Prison admin view").exec(getToken)
    .repeat(1).on(feed(prisonCodes), overviewByPrison(), retrieveForPrison(25))

  init {
    setUp(
      overview.injectOpen(atOnceUsers(1), rampUsersPerSec(0.1).to(10.0).during(ofMinutes(5))),
    ).protocols(httpProtocol)
  }
}
