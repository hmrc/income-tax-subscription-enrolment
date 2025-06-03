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

package controllers

import models.EnrolmentDetails
import org.apache.pekko.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{CREATED, BAD_REQUEST}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

class EnrolmentControllerSpec extends AnyWordSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("Sys")
  val validEnrolmentDetails = EnrolmentDetails(utr = "1234567890", nino = "AA123456A", mtdbsa = "XABC0000000001")

  val controller = new EnrolmentController(Helpers.stubControllerComponents())

  "Enrol" should {
    "return 201 with valid input body" in {
      val fakeRequest = FakeRequest().withBody(Json.toJson(validEnrolmentDetails))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe CREATED
    }

    "return Bad Request with invalid input body" in {
      val fakeRequest = FakeRequest().withBody(Json.toJson(""))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe BAD_REQUEST
    }

  }
}
