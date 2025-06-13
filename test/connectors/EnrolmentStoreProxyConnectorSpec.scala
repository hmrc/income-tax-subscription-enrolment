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

import config.AppConfig
import connectors.httpparsers.UpsertEnrolmentResponseHttpParser.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import base.{TestData, TestGen}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends AnyWordSpec with Matchers with TestData with TestGen {
  
  private val mockHttp = mock[HttpClientV2]
  private val mockRequest = mock[RequestBuilder]
  private val mockConfig = mock[AppConfig]

  private val connector = new EnrolmentStoreProxyConnector(
    mockHttp,
    mockConfig
  )

  when(mockHttp.put(any())(any())).thenReturn(mockRequest)
  when(mockRequest.withBody(any())(any(), any(), any())).thenReturn(mockRequest)
  when(mockConfig.upsertEnrolmentEnrolmentStoreUrl(any())).thenReturn(new URL("http://localhost:8080/"))

  "upsertEnrolment" should {
    "return success when response to ES6 call is NO_CONTENT" in {
      when(mockRequest.execute).thenReturn(
        Future.successful(HttpResponse(NO_CONTENT))
      )
      val result = await(connector.upsertEnrolment(mtdbsa, nino))
      result mustBe Right(UpsertEnrolmentSuccess)
    }

    "return failure when response to ES6 call is other than NO_CONTENT" in {
      val response = UpsertEnrolmentFailure(
        status = statuses.filter(_ != NO_CONTENT).random,
        message = randomString
      )
      when(mockRequest.execute).thenReturn(
        Future.successful(HttpResponse(
          status = response.status,
          body = response.message
        ))
      )
      val result = await(connector.upsertEnrolment(mtdbsa, nino))
      result mustBe Left(response)
    }
  }
}
