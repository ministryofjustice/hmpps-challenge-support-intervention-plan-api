package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

object LanguageFormatUtils {
  fun formatDisplayName(userName: String): String = userName.replace("\\s+|_".toRegex(), " ").split("(?<=[-\\s])|(?=[-\\s])".toRegex())
    .joinToString("") { it.lowercase().replaceFirstChar { char -> char.uppercaseChar() } }
}
