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

import base.TestData
import com.github.tomakehurst.wiremock.client.WireMock
import config.AppConfig
import config.featureswitch.FeatureSwitch.CompositeEnrolmentKey
import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import models.EnrolmentDetails
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import stubs.EnrolmentStoreProxyStubs.{stubES0, stubES1, stubES11, stubES6, stubES8}
import stubs.UsersGroupSearchStubs.stubUGS
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import java.util.UUID

class EnrolmentDetailsIntegrationSpec extends ComponentSpecBase with FeatureSwitching with TestData {

  override val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val validEnrolmentDetails = EnrolmentDetails(
    utr = utr,
    nino = nino,
    mtdbsa = mtdbsa
  )

  override def overriddenConfig(): Map[String, String] = Map(
    "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
    "microservice.services.enrolment-store-proxy.port" -> wireMockPort.toString,
    "microservice.services.users-groups-search.host" -> wireMockHost,
    "microservice.services.users-groups-search.port" -> wireMockPort.toString
  )

  private val correlationId = UUID.randomUUID().toString

  private def setup(useCompositeKey: Boolean, apiToFail: String = ""): Map[String, Seq[String]] = {
    WireMock.reset()
    Map(
      "ES6" -> stubES6(apiToFail == "ES6", appConfig, mtdbsa),
      "ES1" -> stubES1(apiToFail == "ES1", appConfig, utr, groupId),
      "ES0" -> stubES0(apiToFail == "ES0", appConfig, utr, userIds),
      "UGS" -> stubUGS(apiToFail == "UGS", appConfig, groupId, userIds),
      "ES8" -> stubES8(apiToFail == "ES8", appConfig, groupId, mtdbsa, if (useCompositeKey) Some(utr) else None),
      "ES11" -> stubES11(apiToFail == "ES11", appConfig, userIds, mtdbsa)
    )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(CompositeEnrolmentKey)
  }

  "enrol" should {
    "respond with 201 status if all APIs succeed" in {
      Seq(false, true).foreach { useCompositeKey =>
        if (useCompositeKey) {
          enable(CompositeEnrolmentKey)
          info("[CompositeEnrolmentKey] is enabled")
        } else {
          disable(CompositeEnrolmentKey)
          info("[CompositeEnrolmentKey] is disabled")
        }
        val urls = setup(useCompositeKey)
        val response = await(
          buildClient("/enrol")
            .withHttpHeaders("correlationId" -> correlationId)
            .post(Json.toJson(validEnrolmentDetails))
        )

        response.status shouldBe CREATED
        response.body.contains("Failure") shouldBe false

        val ES8Url = urls.get("ES8")
        ES8Url match {
          case Some(urls) if urls.size == 1 => urls.head.contains(utr) shouldBe useCompositeKey
          case _ => fail()
        }
      }
    }

    "respond with an error if one of the APIs fails" in {
      apis.foreach { apiToFail =>
        setup(false, apiToFail)
        info(s"Failing: [$apiToFail]")
        val response = await(
          buildClient("/enrol")
            .withHttpHeaders("correlationId" -> correlationId)
            .post(Json.toJson(validEnrolmentDetails))
        )

        val status = apiToFail match {
          case "ES6" => UNPROCESSABLE_ENTITY
          case _ => CREATED
        }

        response.status shouldBe status
        if (status == CREATED) {
          response.body.contains("Failure") shouldBe true
        }
      }
    }
  }
}
