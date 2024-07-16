package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.areaOfWork
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.contributoryFactorType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.csipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.incidentInvolvement
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.incidentLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.incidentType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.referral
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class CsipRecordServiceTest {
  @Mock
  private lateinit var referenceDataRepository: ReferenceDataRepository

  @Mock
  private lateinit var csipRecordRepository: CsipRecordRepository

  @Mock
  private lateinit var prisonerSearchClient: PrisonerSearchClient

  @InjectMocks
  private lateinit var csipRecordService: CsipRecordService

  @Captor
  private lateinit var csipRecordArgumentCaptor: ArgumentCaptor<CsipRecord>

  @Captor
  private lateinit var contributoryFactorsArgumentCaptor: ArgumentCaptor<List<ContributoryFactor>>

  @Captor
  private lateinit var referralArgumentCaptor: ArgumentCaptor<Referral>

  @Test
  fun `throws exception if prisoner not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("Prisoner number invalid")
  }

  @Test
  fun `throws exception if incident type not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("INCIDENT_TYPE is invalid")
  }

  @Test
  fun `throws exception if incident location not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("INCIDENT_LOCATION is invalid")
  }

  @Test
  fun `throws exception if referrer area of work not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(
      incidentLocation(),
    )
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("AREA_OF_WORK is invalid")
  }

  @Test
  fun `throws exception if incident involvement not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(
      incidentLocation(),
    )
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(areaOfWork())
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("AREA_OF_WORK is invalid")
  }

  @Test
  fun `throws exception if contributory factor not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(
      incidentLocation(),
    )
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(areaOfWork())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_INVOLVEMENT), anyString())).thenReturn(
      incidentInvolvement(),
    )
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(createCsipRecordRequest(), "ABC12345", csipRequestContext())
    }
    assertThat(exception.message).isEqualTo("CONTRIBUTORY_FACTOR_TYPE is invalid")
  }

  @Test
  fun `should save all elements`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(
      incidentLocation(),
    )
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(areaOfWork())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_INVOLVEMENT), anyString())).thenReturn(
      incidentInvolvement(),
    )
    whenever(referenceDataRepository.findByDomainAndCodeIn(eq(CONTRIBUTORY_FACTOR_TYPE), anySet())).thenReturn(
      listOf(contributoryFactorType()),
    )
    whenever(csipRecordRepository.save(any())).thenReturn(
      csipRecord().apply {
        referral = referral(csipRecord())
      },
    )
    csipRecordService.createCsipRecord(createCsipRecordRequest(), PRISON_NUMBER, csipRequestContext())
    verify(csipRecordRepository).save(csipRecordArgumentCaptor.capture())

    with(csipRecordArgumentCaptor.value) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(prisonNumber).isEqualTo(PRISON_NUMBER)
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `throws exception if contributory factors is empty and source is DPS`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    val exception = assertThrows<IllegalArgumentException> {
      csipRecordService.createCsipRecord(
        createCsipRecordRequest(contributoryFactorTypeCode = listOf()),
        "A1234BC",
        csipRequestContext(),
      )
    }
    assertThat(exception.message).isEqualTo("A referral must have >=1 contributory factor(s).")
  }

  @Test
  fun `should save all elements if contributory factors is empty and source is NOMIS`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_TYPE), anyString())).thenReturn(incidentType())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_LOCATION), anyString())).thenReturn(
      incidentLocation(),
    )
    whenever(referenceDataRepository.findByDomainAndCode(eq(AREA_OF_WORK), anyString())).thenReturn(areaOfWork())
    whenever(referenceDataRepository.findByDomainAndCode(eq(INCIDENT_INVOLVEMENT), anyString())).thenReturn(
      incidentInvolvement(),
    )
    whenever(csipRecordRepository.save(any())).thenReturn(csipRecord().apply { referral = referral(csipRecord()) })
    csipRecordService.createCsipRecord(
      createCsipRecordRequest(contributoryFactorTypeCode = listOf()),
      PRISON_NUMBER,
      csipRequestContext().copy(source = Source.NOMIS, username = NOMIS_SYS_USER),
    )
    verify(csipRecordRepository).save(csipRecordArgumentCaptor.capture())

    with(csipRecordArgumentCaptor.value) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(prisonNumber).isEqualTo(PRISON_NUMBER)
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
  }
}
