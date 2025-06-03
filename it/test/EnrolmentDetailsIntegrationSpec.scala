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

import controllers.EnrolmentController
import helpers.ComponentSpecBase
import models.EnrolmentDetails
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class EnrolmentDetailsIntegrationSpec
  extends ComponentSpecBase {

  val validEnrolmentDetails = EnrolmentDetails(utr = "1234567890", nino = "AA123456A", mtdbsa = "XABC0000000001")
  val correlationId = "f0bd1f32-de51-45cc-9b18-0520d6e3ab1a"

  trait Setup {
    val controller: EnrolmentController = app.injector.instanceOf[EnrolmentController]
  }

  "enrol" should {
    "respond with 201 status" in new Setup {
      val response = await(
        buildClient("/enrol")
        .withHttpHeaders("correlationId" -> correlationId)
        .post(Json.toJson(validEnrolmentDetails))
      )

      response.status shouldBe CREATED
    }
  }
}
