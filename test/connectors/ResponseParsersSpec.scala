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
import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, EnrolmentSuccess, INVALID_JSON}
import connectors.ResponseParsers.EnrolmentStoreProxyResponseParser
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse

class ResponseParsersSpec extends AnyWordSpec with Matchers with TestData with TestGen {

  "ResponseParser" should {
    "return success when response is NO_CONTENT" in {
      val httpResponse = HttpResponse(NO_CONTENT)
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Right(EnrolmentSuccess)
    }

    "return failure when response is other than OK and NO_CONTENT" in {
      val response = EnrolmentFailure(
        status = statuses.filter(_ != NO_CONTENT).filter(_ != OK).random,
        message = randomString
      )
      val httpResponse = HttpResponse(response.status, response.message)
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Left(response)
    }

    "return success with 'groupId' when response is OK and contains valid JSON" in {
      val groupId = randomString
      val response = EnrolmentAllocated(groupId)
      val httpResponse = HttpResponse(
        status = OK,
        headers = Map("content-type" -> Seq("application/json")),
        body = s"{\"principalGroupIds\":[\"$groupId\"]}"
      )
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Right(response)
    }

    "return failure when response is OK and contains invalid JSON" in {
      val response = EnrolmentFailure(INVALID_JSON, "")
      val httpResponse = HttpResponse(
        status = OK,
        headers = Map("content-type" -> Seq("application/json")),
        body = s"{\"principalGroupIds\":\"$randomString\"}"
      )
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Left(response)
    }
  }
}
