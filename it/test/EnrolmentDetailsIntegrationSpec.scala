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
import config.AppConfig
import helpers.ComponentSpecBase
import models.EnrolmentDetails
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import stubs.EnrolmentStoreProxyStubs.{stubES0, stubES1, stubES11, stubES6, stubES8}
import stubs.UsersGroupSearchStubs.stubUGS

import java.util.UUID

class EnrolmentDetailsIntegrationSpec extends ComponentSpecBase with TestData {

  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

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

  private def setup(apiToFail: String = ""): Unit = {
    stubES6(apiToFail == "ES6", appConfig, mtdbsa)
    stubES1(apiToFail == "ES1", appConfig, utr, groupId)
    stubES0(apiToFail == "ES0", appConfig, utr, userIds)
    stubUGS(apiToFail == "UGS", appConfig, groupId, userIds)
    stubES8(apiToFail == "ES8", appConfig, groupId, mtdbsa)
    stubES11(apiToFail == "ES11", appConfig, userIds, mtdbsa)
  }

  "enrol" should {
    "respond with 201 status if all APIs succeed" in {
      setup()
      val response = await(
        buildClient("/enrol")
          .withHttpHeaders("correlationId" -> correlationId)
          .post(Json.toJson(validEnrolmentDetails))
      )

      response.status shouldBe CREATED
      response.body.contains("Failure") shouldBe false
    }

    "respond with an error if one of the APIs fails" in {
      apis.foreach { apiToFail =>
        setup(apiToFail)
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
