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

import connectors.{EnrolmentStoreProxyConnector, TestConnector}
import models.{EnrolmentError, Outcome}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(
  testConnector: TestConnector,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit ec: ExecutionContext) extends Logging {

  def enrol(
    utr: String,
    nino: String,
    mtdbsa: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, Seq[Outcome]]] = {
    val resultEmpty = Right(ServiceSuccess(Seq.empty, testConnector.setup()))
    for {
      resultES6 <- getResult("ES6", resultEmpty, mtdbsa, nino, utr, upsertEnrolmentAllocation)
      resultAll <- getResult("other", resultES6, mtdbsa, nino, utr, someOtherAction)
    } yield {
      resultAll match {
        case Right(value) => Right(value.outcomes)
        case Left(error) => Left(error)
      }
    }
  }

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def getResult(
    apiName: String,
    result: Either[ServiceFailure, ServiceSuccess],
    mtdbsa: String,
    nino: String,
    utr: String,
    function: (String, Map[String, String], Seq[Outcome], String, String, String) => Future[Either[ServiceFailure, ServiceSuccess]]
  ): Future[Either[ServiceFailure, ServiceSuccess]] = result match {
    case Right(success) => function(
      apiName,
      success.data,
      success.outcomes,
      mtdbsa,
      nino,
      utr
    )
    case Left(_) => Future.successful(
      result
    )
  }

  private def upsertEnrolmentAllocation(
    apiName: String,
    data: Map[String, String],
    outcomes: Seq[Outcome],
    mtdbsa: String,
    nino: String,
    utr: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceSuccess]] = {
    enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
      case Right(_) =>
        Right(ServiceSuccess(
          outcomes = outcomes :+ Outcome.success(apiName),
          data = data
        ))
      case Left(EnrolmentStoreProxyConnector.UpsertEnrolmentFailure(status, message)) =>
        logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
        Left(ServiceFailure(
          error = Some(EnrolmentError(status.toString, message)))
        )
    }
  }

  private def someOtherAction(
    apiName: String,
    data: Map[String, String],
    outcomes: Seq[Outcome],
    mtdbsa: String,
    nino: String,
    utr: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceSuccess]] = {
    data.get(TestConnector.key) match {
      case Some(value) => testConnector.someOtherAction(value).map {
        case true =>
          Right(ServiceSuccess(
            outcomes = outcomes :+ Outcome.success(apiName),
            data = data ++ Map(apiName -> "")
          ))
        case false =>
          logError("someOtherAction", "", "")
          Left(ServiceFailure(
            outcomes = outcomes :+ Outcome(apiName, "fail")
          ))
      }
      case None => Future.successful(
        Left(ServiceFailure(
          outcomes = outcomes :+ Outcome(apiName, "fail")
        ))
      )
    }
  }
}

case class ServiceSuccess(
  outcomes: Seq[Outcome],
  data: Map[String, String] = Map.empty
)

case class ServiceFailure(
  outcomes: Seq[Outcome] = Seq.empty,
  error: Option[EnrolmentError] = None
)
