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
import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, EnrolmentSuccess}
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
  )(implicit hc: HeaderCarrier): Future[Either[Failure, Seq[Outcome]]] = {
    val result = SuccessBase()
    for {
      resultES6 <- upsertEnrolmentAllocation(result, mtdbsa, nino)
      resultES1 <- getGroupIdForEnrolment(resultES6, utr, nino)
    } yield {
      resultES1.outcomes
    }
  }.value

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def upsertEnrolmentAllocation(
    result: Success,
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Failure, SuccessES6] = {
    val serviceName = "HMRC-MTD-IT"
    val identifiers = "MTDITID" -> mtdbsa
    EitherT {
      enrolmentStoreProxyConnector.upsertEnrolment(serviceName, identifiers, nino).map {
        case Right(_) =>
          Right(SuccessES6(
            outcomes = result.outcomes :+ Outcome.success("ES6")
          ))
        case Left(EnrolmentStoreProxyConnector.EnrolmentFailure(status, message)) =>
          logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
          Left(Failure(
            error = Some(EnrolmentError(status.toString, message)))
          )
      }
    }
  }

  private def getGroupIdForEnrolment(
    result: SuccessES6,
    utr: String,
    nino: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Failure, SuccessES1] = {
    val apiName = "ES1"
    val outcomes = result.outcomes
    val serviceName = "IR-SA"
    val identifiers = "UTR" -> utr
    EitherT {
      val location = "getGroupIdForEnrolment"
      enrolmentStoreProxyConnector.getAllocatedEnrolments(serviceName, identifiers) map {
        case Right(EnrolmentSuccess) =>
          val message = "Enrolment not allocated"
          logError(location, nino, message)
          Left(Failure(
            outcomes = outcomes :+ Outcome.failure(apiName, message)
          ))
        case Right(EnrolmentAllocated(groupId)) =>
          Right(SuccessES1(
            outcomes = outcomes :+ Outcome.success(apiName),
            groupId = groupId
          ))
        case Left(EnrolmentFailure(_, message)) =>
          logError(location, nino, message)
          Left(Failure(
            outcomes = outcomes :+ Outcome.failure(apiName, message)
          ))
      }
    }
  }
}

trait Success {
  def outcomes: Seq[Outcome]
}

case class SuccessBase(
  outcomes: Seq[Outcome] = Seq.empty
) extends Success

case class SuccessES6(
  outcomes: Seq[Outcome]
) extends Success

case class ServiceSuccessOther(
  outcomes: Seq[Outcome],
  data: String
) extends Success

case class SuccessES1(
  outcomes: Seq[Outcome],
  groupId: String
) extends Success

case class Failure(
  outcomes: Seq[Outcome] = Seq.empty,
  error: Option[EnrolmentError] = None
)
