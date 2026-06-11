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

package config

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait MockConfig extends MockitoSugar {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockConfiguration: Configuration = mock[Configuration]

  when(mockConfiguration.get[String]("appName"))
    .thenReturn("app")

  when(mockServicesConfig.baseUrl("enrolment-store-proxy"))
    .thenReturn("http://localhost:9564")

  when(mockServicesConfig.baseUrl("users-groups-search"))
    .thenReturn("http://localhost:9564")

  implicit val appConfig: AppConfig =
    new AppConfig(
      config = mockServicesConfig,
      configuration = mockConfiguration
    )
}
