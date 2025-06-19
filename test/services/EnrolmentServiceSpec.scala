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

package services

import connectors.EnrolmentStoreProxyConnector
import models.{EnrolmentError, Outcome}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import base.TestData
import com.gargoylesoftware.htmlunit.javascript.host.event.BeforeUnloadEvent
import connectors.EnrolmentStoreProxyConnector.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentServiceSpec extends AnyWordSpec with Matchers with TestData with BeforeAndAfterEach {

  val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  private val mockConnector = mock[EnrolmentStoreProxyConnector]

  private val service = new EnrolmentService(
    mockConnector
  )(executionContext)

  private def setup() = {
    reset(mockConnector)
    when(mockConnector.someOtherAction).thenReturn(
      Future.successful(true)
    )
  }

  "enrol" should {
    "return success when ES6 succeeds" in {
      setup()
      when(mockConnector.upsertEnrolment(any(), any())(any())).thenReturn(
        Future.successful(Right(UpsertEnrolmentSuccess))
      )
      val result = await(service.enrol(utr, nino, mtdbsa))
      result match {
        case Right(outcomes) =>
          outcomes.contains(Outcome.success("ES6")) mustBe true
          verify(mockConnector, times(1)).upsertEnrolment(any(), any())(any())
          verify(mockConnector, times(1)).someOtherAction
        case Left(_) =>
          fail
      }
    }

    "return failure when ES6 fails" in {
      setup();
      val error = UpsertEnrolmentFailure(SERVICE_UNAVAILABLE, "")
      when(mockConnector.upsertEnrolment(any(), any())(any())).thenReturn(
        Future.successful(Left(error))
      )
      val result = await(service.enrol(utr, nino, mtdbsa))
      result mustBe Left(ServiceFailure(
        error = Some(error.asError())
      ))
      verify(mockConnector, times(1)).upsertEnrolment(any(), any())(any())
      verify(mockConnector, times(0)).someOtherAction
    }
  }

  implicit class Converter(response: UpsertEnrolmentFailure) {
    def asError(): EnrolmentError = EnrolmentError(
      code = response.status.toString,
      message = response.message
    )
  }
}
