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
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit ec: ExecutionContext) extends Logging {

  def enrol(
    utr: String,
    nino: String,
    mtdbsa: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, Seq[Outcome]]] = {
    val result = Right(ServiceSuccess(Seq.empty))
    for {
      result <- upsertEnrolmentAllocation(result, mtdbsa, nino)
      result <- someOtherAction(result, mtdbsa, nino, utr)
    } yield {
      result match {
        case Right(value) => Right(value.outcomes)
        case Left(error) => Left(error)
      }
    }
  }

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def getOutcomesFromResult(
    result: Either[ServiceFailure, ServiceResponse]
  ): Option[Seq[Outcome]] = {
    result match {
      case Right(value) => Some(value.outcomes)
      case Left(_) => None
    }
  }

  private def upsertEnrolmentAllocation(
    result: Either[ServiceFailure, ServiceResponse],
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceResponse]] = {
    getOutcomesFromResult(result) match {
      case Some(outcomes) =>
        enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
          case Right(_) =>
            Right(ServiceSuccess(outcomes :+ Outcome.success("ES6")))
          case Left(EnrolmentStoreProxyConnector.UpsertEnrolmentFailure(status, message)) =>
            logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
            Left(ServiceFailure(
              error = Some(EnrolmentError(status.toString, message)))
            )
        }
      case None => Future.successful(result)
    }
  }

  private def someOtherAction(
    result: Either[ServiceFailure, ServiceResponse],
    mtdbsa: String,
    nino: String,
    utr: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceResponse]] = {
    getOutcomesFromResult(result) match {
      case Some(outcomes) =>
        enrolmentStoreProxyConnector.someOtherAction.map {
          case true =>
            Right(ServiceSuccessWithData[Int](
              outcomes = outcomes :+ Outcome.success("other"),
              data = 1
            ))
          case false =>
            logError("someOtherAction", nino, "")
            Left(ServiceFailure(
              outcomes = outcomes :+ Outcome("other", "fail")
            ))
        }
      case None => Future.successful(result)
    }
  }
}

trait ServiceResponse {
  def outcomes: Seq[Outcome]
}

case class ServiceSuccess(
  outcomes: Seq[Outcome]
) extends ServiceResponse

case class ServiceSuccessWithData[A](
  outcomes: Seq[Outcome],
  data: A
) extends ServiceResponse

case class ServiceFailure(
  error: Option[EnrolmentError] = None,
  outcomes: Seq[Outcome] = Seq.empty
)
