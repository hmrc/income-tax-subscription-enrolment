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
import helpers.{ComponentSpecBase, WiremockHelper}
import models.EnrolmentDetails
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import stubs.EnrolmentStoreProxyStubs.stubEnrolmentStoreProxy

import java.util.UUID

class EnrolmentDetailsIntegrationSpec extends ComponentSpecBase with TestData {

  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val validEnrolmentDetails = EnrolmentDetails(
    utr = utr,
    nino = nino,
    mtdbsa = mtdbsa
  )

  override def overriddenConfig: Map[String, String] = Map(
    "microservice.services.enrolment-store-proxy.host" -> mockHost,
    "microservice.services.enrolment-store-proxy.port" -> mockPort
  )

  private val correlationId = UUID.randomUUID().toString

  "enrol" should {
    "respond with 201 status" in {
      stubEnrolmentStoreProxy(appConfig, mtdbsa)
      val response = await(
        buildClient("/enrol")
          .withHttpHeaders("correlationId" -> correlationId)
          .post(Json.toJson(validEnrolmentDetails))
      )

      response.status shouldBe CREATED
    }
  }
}
