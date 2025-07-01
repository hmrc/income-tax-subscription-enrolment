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
import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, EnrolmentSuccess, UsersFound}
import connectors.UsersGroupsSearchConnector.GroupUsersFound
import connectors.{EnrolmentStoreProxyConnector, UsersGroupsSearchConnector}
import models.{EnrolmentError, Outcome}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
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

  private val mockEnrolConnector = mock[EnrolmentStoreProxyConnector]
  private val mockGroupConnector = mock[UsersGroupsSearchConnector]

  private val service = new EnrolmentService(
    mockEnrolConnector,
    mockGroupConnector
  )(executionContext)

  private def setup() = {
    reset(mockEnrolConnector)
    reset(mockGroupConnector)
    when(mockEnrolConnector.upsertEnrolment(any(), any())(any())).thenReturn(
      Future.successful(Right(EnrolmentSuccess))
    )
    when(mockEnrolConnector.getAllocatedEnrolments(any())(any())).thenReturn(
      Future.successful(Right(EnrolmentAllocated(groupId)))
    )
    when(mockEnrolConnector.getUserIds(any())(any())).thenReturn(
      Future.successful(Right(UsersFound(userIds)))
    )
    when(mockGroupConnector.getUsersForGroup(any())(any)).thenReturn(
      Future.successful(Right(GroupUsersFound(userIds.toSeq)))
    )
  }

  "enrol" should {
    "return success when all APIs succeed" in {
      setup();
      val result = await(service.enrol(utr, nino, mtdbsa))
      val allAPIs = Seq("ES6") ++ otherAPIs
      info(s"Succeeding [${allAPIs.mkString(", ")}]")
      val expected = allAPIs.map(Outcome.success)
      result match {
        case Right(outcomes) =>
          outcomes mustBe expected
          verify(mockEnrolConnector, times(1)).upsertEnrolment(any(), any())(any())
          verify(mockEnrolConnector, times(1)).getAllocatedEnrolments(any())(any())
          verify(mockEnrolConnector, times(1)).getUserIds(any())(any())
        case Left(_) =>
          fail()
      }
    }

    "return failure with error when ES6 fails" in {
      setup();
      val error = EnrolmentFailure(SERVICE_UNAVAILABLE, "")
      when(mockEnrolConnector.upsertEnrolment(any(), any())(any())).thenReturn(
        Future.successful(Left(error))
      )
      val result = await(service.enrol(utr, nino, mtdbsa))
      result mustBe Left(Failure(
        error = Some(error.asError())
      ))
      verify(mockEnrolConnector, times(1)).upsertEnrolment(any(), any())(any())
      verify(mockEnrolConnector, times(0)).getAllocatedEnrolments(any())(any())
      verify(mockEnrolConnector, times(0)).getUserIds(any())(any())
    }

    "return failure without error if other APIs fail" in {
      otherAPIs.foreach { api =>
        setup()
        val message = failAPI(api)
        val result = await(service.enrol(utr, nino, mtdbsa))
        result match {
          case Right(_) =>
            fail()
          case Left(failure) if failure.error.isEmpty =>
            val outcomes = failure.outcomes
            outcomes.head mustBe Outcome.success("ES6")
            outcomes.last mustBe Outcome.failure(api, message)
          case Left(_) =>
            fail()
        }
      }
    }

    "return unexpected error if result unexpected" in {
      Seq("ES1", "ES0").foreach { api =>
        setup();
        api match {
          case "ES1" =>
            when(mockEnrolConnector.getAllocatedEnrolments(any())(any())).thenReturn(
              Future.successful(Right(EnrolmentSuccess))
            )
          case "ES0" =>
            when(mockEnrolConnector.getUserIds(any())(any())).thenReturn(
              Future.successful(Right(EnrolmentSuccess))
            )
        }
        val result = await(service.enrol(utr, nino, mtdbsa))
        result match {
          case Right(_) => fail()
          case Left(failure) if failure.error.isEmpty =>
            val last = failure.outcomes.last
            last.status.contains("Unexpected") mustBe true
          case _ => fail()
        }
      }
    }
  }

  implicit class Converter(response: EnrolmentFailure) {
    def asError(): EnrolmentError = EnrolmentError(
      code = response.status.toString,
      message = response.message
    )
  }

  private val otherAPIs = Seq(
    "ES1",
    "ES0",
    "UGS"
  )

  private def failAPI(api: String): String = {
    val message = api match {
      case "ES1" => failES1
      case "ES0" => failES0
      case "UGS" => failUGS
      case _ =>
        throw new Exception(s"Unknown API: $api")
    }
    info(s"Failing [$api]")
    message
  }

  private def failES1: String = {
    val message = "Service unavailable"
    when(mockEnrolConnector.getAllocatedEnrolments(any())(any())).thenReturn(
      Future.successful(Left(EnrolmentFailure(SERVICE_UNAVAILABLE, message)))
    )
    message
  }

  private def failES0: String = {
    val message = "Service unavailable"
    when(mockEnrolConnector.getUserIds(any())(any())).thenReturn(
      Future.successful(Left(EnrolmentFailure(SERVICE_UNAVAILABLE, message)))
    )
    message
  }

  private def failUGS: String = {
    when(mockGroupConnector.getUsersForGroup(any())(any())).thenReturn(
      Future.successful(Right(GroupUsersFound(Seq.empty)))
    )
    s"No ADMIN users for group: $groupId"
  }
}
