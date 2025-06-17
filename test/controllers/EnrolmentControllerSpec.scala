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

import base.TestData
import models.{EnrolmentDetails, EnrolmentError, EnrolmentResponse, Outcome}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolmentService, ServiceOutcome}

import scala.concurrent.Future

class EnrolmentControllerSpec extends AnyWordSpec with Matchers with TestData {

  private val validEnrolmentDetails = EnrolmentDetails(
    utr = utr,
    nino = nino,
    mtdbsa = mtdbsa
  )

  private val mockService = mock[EnrolmentService]

  private val controller = new EnrolmentController(
    mockService
  )

  "enrol" should {
    "return 201 when all downstream APIs succeed" in {
      val response = Seq(Outcome.success("ES6"))
      when(mockService.enrol(any(), any(), any())(any())).thenReturn(
        Future.successful(Right(response))
      )
      val fakeRequest = FakeRequest().withBody(Json.toJson(validEnrolmentDetails))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe CREATED
      Json.fromJson[EnrolmentResponse](contentAsJson(result)) shouldBe JsSuccess(EnrolmentResponse(response))
    }

    "return 422 when ES6 fails" in {
      val error = EnrolmentError("404", "")
      when(mockService.enrol(any(), any(), any())(any())).thenReturn(
        Future.successful(Left(ServiceOutcome(
          error = Some(error)
        )))
      )
      val fakeRequest = FakeRequest().withBody(Json.toJson(validEnrolmentDetails))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe UNPROCESSABLE_ENTITY
      Json.fromJson[EnrolmentError](contentAsJson(result)) shouldBe JsSuccess(error)
    }

    "return 201 when ES6 succeeds and other APIs fail" in {
      val response = Seq(Outcome.success("ES6"), Outcome("others", "fail"))
      when(mockService.enrol(any(), any(), any())(any())).thenReturn(
        Future.successful(Left(ServiceOutcome(
          outcomes = response
        )))
      )
      val fakeRequest = FakeRequest().withBody(Json.toJson(validEnrolmentDetails))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe CREATED
      Json.fromJson[EnrolmentResponse](contentAsJson(result)) shouldBe JsSuccess(EnrolmentResponse(response))
    }

    "return 401 when request is invalid" in {
      val fakeRequest = FakeRequest().withBody(Json.toJson(""))
      val result = controller.enrol()(fakeRequest)
      status(result) shouldBe BAD_REQUEST
    }
  }
}
