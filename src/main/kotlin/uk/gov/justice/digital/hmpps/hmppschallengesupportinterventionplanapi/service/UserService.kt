package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers.ManageUsersClient

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {
  fun getUserDetails(username: String) = manageUsersClient.getUserDetails(username)
}
