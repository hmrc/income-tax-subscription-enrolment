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

import cats.data.EitherT
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
    val result = ServiceSuccess(Seq.empty)
    for {
      result <- upsertEnrolmentAllocation(result, mtdbsa, nino)
      result <- someOtherAction(result, result.outcomes.head.api)
    } yield {
      result.outcomes
    }
  }.value

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def upsertEnrolmentAllocation(
    result: ServiceAbstract,
    mtdbsa: String,
    nino: String,
  )(implicit hc: HeaderCarrier): EitherT[Future, ServiceFailure, ServiceSuccess] = {
    EitherT {
      enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
        case Right(_) =>
          Right(ServiceSuccess(
            outcomes = result.outcomes :+ Outcome.success("ES6"),
          ))
        case Left(EnrolmentStoreProxyConnector.UpsertEnrolmentFailure(status, message)) =>
          logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
          Left(ServiceFailure(
            error = Some(EnrolmentError(status.toString, message)))
          )
      }
    }
  }

  private def someOtherAction(
    result: ServiceAbstract,
    value: String
  ): EitherT[Future, ServiceFailure, ServiceSuccessOther] = {
    val apiName = "other"
    val outcomes = result.outcomes
    EitherT {
      testConnector.someOtherAction(value).map {
        case true =>
          Right(ServiceSuccessOther(
            outcomes = outcomes :+ Outcome.success(apiName),
            data = "1"
          ))
        case false =>
          logError("someOtherAction", "", "")
          Left(ServiceFailure(
            outcomes = outcomes :+ Outcome(apiName, "fail")
          ))
      }
    }
  }
}

trait ServiceAbstract {
  def outcomes: Seq[Outcome]
}

case class ServiceSuccess(
  outcomes: Seq[Outcome]
) extends ServiceAbstract

case class ServiceSuccessOther(
  outcomes: Seq[Outcome],
  data: String
) extends ServiceAbstract

case class ServiceFailure(
  outcomes: Seq[Outcome] = Seq.empty,
  error: Option[EnrolmentError] = None
)
