/*
 * Copyright 2018 HM Revenue & Customs
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

package helpers

import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

trait ComponentSpecBase extends AnyWordSpecLike
  with OptionValues
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers with Assertions
  with WireMockSupport with BeforeAndAfterEach with BeforeAndAfterAll with Eventually {

  private lazy val ws = app.injector.instanceOf[WSClient]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  def config: Map[String, String] = Map(
    "microservice.services.auth.host" -> wireMockHost,
    "microservice.services.auth.port" -> wireMockPort.toString
  ) ++ overriddenConfig()

  def overriddenConfig(): Map[String, String] = Map.empty

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def buildClient(path: String): WSRequest =
    ws.url(s"http://localhost:$port/income-tax-subscription-enrolment$path")
      .withFollowRedirects(false)
}
