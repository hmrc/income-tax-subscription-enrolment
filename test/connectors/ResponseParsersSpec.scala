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
import connectors.EnrolmentStoreProxyConnector.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import connectors.ResponseParsers.EnrolmentStoreProxyResponseParser
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse

class ResponseParsersSpec extends AnyWordSpec with Matchers with TestData with TestGen {

  "EnrolmentStoreProxyResponseParser" should {
    "return success when response to ES6 call is NO_CONTENT" in {
      val httpResponse = HttpResponse(NO_CONTENT)
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Right(UpsertEnrolmentSuccess)
    }

    "return failure when response to ES6 call is other than NO_CONTENT" in {
      val response = UpsertEnrolmentFailure(
        status = statuses.filter(_ != NO_CONTENT).random,
        message = randomString
      )
      val httpResponse = HttpResponse(response.status, response.message)
      val result = EnrolmentStoreProxyResponseParser.read("", "", httpResponse)
      result mustBe Left(response)
    }
  }
}
