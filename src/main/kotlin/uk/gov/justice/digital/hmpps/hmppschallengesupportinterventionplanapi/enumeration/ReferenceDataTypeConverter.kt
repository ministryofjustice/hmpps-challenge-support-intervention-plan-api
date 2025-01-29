package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import org.springframework.core.convert.converter.Converter

class ReferenceDataTypeConverter : Converter<String, ReferenceDataType> {
  override fun convert(source: String): ReferenceDataType = ReferenceDataType.fromDomain(source)
}
