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

import base.TestData
import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, EnrolmentSuccess}
import connectors.EnrolmentStoreProxyConnector
import models.{EnrolmentError, Outcome}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.Succeeded
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentServiceSpec extends AnyWordSpec with Matchers with TestData {

  val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  private val mockConnector = mock[EnrolmentStoreProxyConnector]

  private val service = new EnrolmentService(
    mockConnector
  )(executionContext)

  private def setup() = {
    reset(mockConnector)
    when(mockConnector.upsertEnrolment(any(), any(), any())(any())).thenReturn(
      Future.successful(Right(EnrolmentSuccess))
    )
    when(mockConnector.getAllocatedEnrolments(any(), any())(any())).thenReturn(
      Future.successful(Right(EnrolmentAllocated(groupId)))
    )
  }

  def check(success: Boolean): Unit = {
    val result = await(service.enrol(utr, nino, mtdbsa))
    result match {
      case Right(_) if !success =>
        fail()
      case Right(outcomes) =>
        outcomes.head mustBe Outcome.success("ES6")
        verify(mockConnector, times(1)).upsertEnrolment(any(), any(), any())(any())
        verify(mockConnector, times(1)).getAllocatedEnrolments(any(), any())(any())
      case Left(_) if success =>
        fail()
      case Left(failure) if failure.error.isDefined =>
        fail()
      case Left(failure) =>
        failure.outcomes.head mustBe Outcome.success("ES6")
        verify(mockConnector, times(1)).upsertEnrolment(any(), any(), any())(any())
        verify(mockConnector, times(1)).getAllocatedEnrolments(any(), any())(any())
    }
  }

  "enrol" when {
    "ES6 succeeds and " should {
      "ES1 succeeds then return success" in {
        setup();
        check(true)
      }

      "ES1 fails then return a failure without error" in {
        setup()
        when(mockConnector.getAllocatedEnrolments(any(), any())(any())).thenReturn(
          Future.successful(Left(EnrolmentFailure(SERVICE_UNAVAILABLE, "")))
        )
        check(false)
      }
    }

    "return failure with error when ES6 fails" in {
      setup();
      val error = EnrolmentFailure(SERVICE_UNAVAILABLE, "")
      when(mockConnector.upsertEnrolment(any(), any(), any())(any())).thenReturn(
        Future.successful(Left(error))
      )
      val result = await(service.enrol(utr, nino, mtdbsa))
      result mustBe Left(Failure(
        error = Some(error.asError())
      ))
      verify(mockConnector, times(1)).upsertEnrolment(any(), any(), any())(any())
      verify(mockConnector, times(0)).getAllocatedEnrolments(any(), any())(any())
    }

    "return failure without error if ES1 fails" in {
      setup()
      when(mockConnector.getAllocatedEnrolments(any(), any())(any())).thenReturn(
        Future.successful(Left(EnrolmentFailure(SERVICE_UNAVAILABLE, "")))
      )
      val result = await(service.enrol(utr, nino, mtdbsa))
      result match {
        case Right(_) => fail()
        case Left(failure) if failure.error.isEmpty => Succeeded
        case Left(_) => fail()
      }
    }
  }

  implicit class Converter(response: EnrolmentFailure) {
    def asError(): EnrolmentError = EnrolmentError(
      code = response.status.toString,
      message = response.message
    )
  }
}
