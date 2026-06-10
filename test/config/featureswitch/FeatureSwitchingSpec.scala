/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config.featureswitch

import config.AppConfig
import config.featureswitch.FeatureSwitch.{CompositeEnrolmentKey, DistributedKnownFactsPattern}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utilities.UnitTestTrait

class FeatureSwitchingSpec extends UnitTestTrait with BeforeAndAfterEach {

  private val switchesUnderTest: Set[FeatureSwitch] = Set(
    CompositeEnrolmentKey,
    DistributedKnownFactsPattern
  )

  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  val mockConfig: Configuration = mock[Configuration]

  val featureSwitching: FeatureSwitching =
    new FeatureSwitching {
      override val appConfig: AppConfig =
        new AppConfig(
          config = servicesConfig,
          configuration = mockConfig
        )
    }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConfig)

    switchesUnderTest.foreach { featureSwitch =>
      sys.props -= featureSwitch.name
    }
  }

  override def afterEach(): Unit = {
    switchesUnderTest.foreach { featureSwitch =>
      sys.props -= featureSwitch.name
    }

    super.afterEach()
  }

  "FeatureSwitching constants" should {
    "be true and false" in {
      FEATURE_SWITCH_ON mustBe "true"
      FEATURE_SWITCH_OFF mustBe "false"
    }
  }

  "CompositeEnrolmentKey" should {
    "return true if CompositeEnrolmentKey feature switch is enabled in sys.props" in {
      enable(CompositeEnrolmentKey)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe true
    }

    "return false if CompositeEnrolmentKey feature switch is disabled in sys.props" in {
      disable(CompositeEnrolmentKey)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return false if CompositeEnrolmentKey feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(None)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return false if CompositeEnrolmentKey feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(Some(FEATURE_SWITCH_OFF))
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return true if CompositeEnrolmentKey feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(Some(FEATURE_SWITCH_ON))
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe true
    }
  }

  "DistributedKnownFactsPattern" should {
    "return true if DistributedKnownFactsPattern feature switch is enabled in sys.props" in {
      enable(DistributedKnownFactsPattern)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe true
    }

    "return false if DistributedKnownFactsPattern feature switch is disabled in sys.props" in {
      disable(DistributedKnownFactsPattern)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return false if DistributedKnownFactsPattern feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(None)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return false if DistributedKnownFactsPattern feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(Some(FEATURE_SWITCH_OFF))
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return true if DistributedKnownFactsPattern feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(Some(FEATURE_SWITCH_ON))
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe true
    }
  }
}