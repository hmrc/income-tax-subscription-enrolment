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

package connectors

import base.{TestData, TestGen}
import connectors.EnrolmentStoreParsers.{AllocateEnrolmentResponseHttpReads, AssignEnrolmentToUserHttpReads, GroupIdResponseParser, UpsertResponseParser, UserIdsResponseParser}
import connectors.EnrolmentStoreProxyConnector.{EnrolFailure, EnrolSuccess, EnrolmentAllocated, EnrolmentAssigned, EnrolmentAssignmentFailure, EnrolmentFailure, EnrolmentSuccess, UsersFound}
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse

class EnrolmentStoreParsersSpec extends AnyWordSpec with Matchers with TestData with TestGen {

  "EnrolmentStoreParsers" when {
    "UpsertResponseParser is invoked" should {
      "return success when response is NO_CONTENT" in {
        val httpResponse = HttpResponse(NO_CONTENT)
        val result = UpsertResponseParser.read("", "", httpResponse)
        result mustBe Right(EnrolmentSuccess)
      }

      "return failure when response is other than NO_CONTENT" in {
        val response = EnrolmentFailure(
          status = statuses.filter(_ != NO_CONTENT).random,
          message = randomString
        )
        val httpResponse = HttpResponse(response.status, response.message)
        val result = UpsertResponseParser.read("", "", httpResponse)
        result mustBe Left(response)
      }
    }

    "GroupIdResponseParser is invoked" should {
      "return success with 'groupId' when response is OK and contains valid JSON" in {
        val groupId = randomString
        val response = EnrolmentAllocated(groupId)
        val httpResponse = HttpResponse(
          status = OK,
          headers = Map("content-type" -> Seq("application/json")),
          body = s"{\"principalGroupIds\":[\"$groupId\"]}"
        )
        val result = GroupIdResponseParser.read("", "", httpResponse)
        result mustBe Right(response)
      }

      "return failure when response is OK and contains invalid JSON" in {
        val response = EnrolmentFailure(INTERNAL_SERVER_ERROR, "Unexpected JSON in response")
        val httpResponse = HttpResponse(
          status = OK,
          headers = Map("content-type" -> Seq("application/json")),
          body = s"{\"principalGroupIds\":\"$randomString\"}"
        )
        val result = GroupIdResponseParser.read("", "", httpResponse)
        result mustBe Left(response)
      }

      "return failure when response is other than OK" in {
        val response = EnrolmentFailure(
          status = statuses.filter(_ != OK).random,
          message = randomString
        )
        val httpResponse = HttpResponse(response.status, response.message)
        val result = GroupIdResponseParser.read("", "", httpResponse)
        result mustBe Left(response)
      }
    }
  }

  "UserIdsResponseParser is invoked" should {
    "return success with 'userIds' when response is OK and contains valid JSON" in {
      val userId1 = randomString
      val userId2 = randomString
      val response = UsersFound(Seq(userId1, userId2))
      val httpResponse = HttpResponse(
        status = OK,
        headers = Map("content-type" -> Seq("application/json")),
        body = s"{\"principalUserIds\":[\"$userId1\",\"$userId2\"]}"
      )
      val result = UserIdsResponseParser.read("", "", httpResponse)
      result mustBe Right(response)
    }

    "return failure when response is OK and contains invalid JSON" in {
      val response = EnrolmentFailure(INTERNAL_SERVER_ERROR, "Unexpected JSON in response")
      val httpResponse = HttpResponse(
        status = OK,
        headers = Map("content-type" -> Seq("application/json")),
        body = s"{\"principalUserIds\":\"$randomString\"}"
      )
      val result = UserIdsResponseParser.read("", "", httpResponse)
      result mustBe Left(response)
    }

    "return failure when response is other than OK" in {
      val response = EnrolmentFailure(
        status = statuses.filter(_ != OK).random,
        message = randomString
      )
      val httpResponse = HttpResponse(response.status, response.message)
      val result = UserIdsResponseParser.read("", "", httpResponse)
      result mustBe Left(response)
    }
  }

  "AllocateEnrolmentResponseHttpReads is invoked" should {
    "return success when response is CREATED" in {
      val httpResponse = HttpResponse(CREATED)
      val result = AllocateEnrolmentResponseHttpReads.read("", "", httpResponse)
      result mustBe Right(EnrolSuccess)
    }

    "return failure when response is other than CREATED" in {
      val status = statuses.filter(_ != CREATED).random
      val response = EnrolFailure(
        message = randomString
      )
      val httpResponse = HttpResponse(status, response.message)
      val result = AllocateEnrolmentResponseHttpReads.read("", "", httpResponse)
      result mustBe Left(response)
    }
  }

  "AssignEnrolmentToUserHttpReads is invoked" should {
    "return success when response is CREATED or NO_CONTENT" in {
      val httpResponse = HttpResponse(Seq(CREATED, NO_CONTENT).random)
      val result = AssignEnrolmentToUserHttpReads.read("", "", httpResponse)
      result mustBe Right(EnrolmentAssigned)
    }

    "return failure when response is other than CREATED or NO_CONTENT" in {
      val response = EnrolmentAssignmentFailure(
        status = statuses.filter(_ != CREATED).filter(_ != NO_CONTENT).random,
        body = randomString
      )
      val httpResponse = HttpResponse(response.status, response.body)
      val result = AssignEnrolmentToUserHttpReads.read("", "", httpResponse)
      result mustBe Left(response)
    }
  }
}
