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
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier

trait ComponentSpecBase extends AnyWordSpecLike
  with OptionValues
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers with Assertions
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl = s"http://$mockHost:$mockPort"

  def config: Map[String, String] = Map(
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort
  ) ++ overriddenConfig()

  def overriddenConfig(): Map[String, String] = Map.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
  }

  override protected def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

}
