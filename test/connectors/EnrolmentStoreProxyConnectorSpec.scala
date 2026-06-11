/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import base.TestData
import config.AppConfig
import config.featureswitch.FeatureSwitch.CompositeEnrolmentKey
import config.featureswitch.FeatureSwitching
import org.apache.pekko.actor.TypedActor.dispatcher
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.client.HttpClientV2

class EnrolmentStoreProxyConnectorSpec extends AnyWordSpec with FeatureSwitching with TestData{

  override val appConfig: AppConfig = mock[AppConfig]

  val connector = new EnrolmentStoreProxyConnector(
    mock[HttpClientV2],
    appConfig
  )

  "enrolmentKey" should {
    "not contain a utr if no utr specified" in {
      val key = connector.enrolmentKey(mtdbsa).asString
      key.contains(utr) mustBe false
    }

    "contain the utr if FS is on" in {
      enable(CompositeEnrolmentKey)
      val key = connector.enrolmentKey(mtdbsa, Some(utr)).asString
      key.contains(utr) mustBe true
    }

    "not contain the utr if FS is off" in {
      disable(CompositeEnrolmentKey)
      val key = connector.enrolmentKey(mtdbsa, Some(utr)).asString
      key.contains(utr) mustBe false
    }
  }
}
