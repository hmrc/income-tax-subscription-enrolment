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
import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, UsersFound}
import connectors.UsersGroupsSearchConnector.{GroupUsersFound, InvalidJson, UsersGroupsSearchConnectionFailure}
import connectors.{EnrolmentStoreProxyConnector, UsersGroupsSearchConnector}
import models.{EnrolmentError, Outcome}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  usersGroutSearchConnector: UsersGroupsSearchConnector
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
      resultES0 <- getUserIdsForEnrolment(resultES1, utr, nino)
      resultUGS <- getAdminUserForGroup(resultES0, nino, resultES1.groupId, resultES0.userIds)
    } yield {
      resultUGS.outcomes
    }
  }.value

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def unexpectedResponse(
    apiName: String,
    outcomes: Seq[Outcome],
    location: String,
    nino: String
  ) = {
    val message = "Unexpected response"
    logError(location, nino, message)
    Left(Failure(
      outcomes = outcomes :+ Outcome.failure(apiName, message)
    ))
  }

  private def upsertEnrolmentAllocation(
    result: Success,
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Failure, SuccessES6] = {
    EitherT {
      enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
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
    EitherT {
      val location = "getGroupIdForEnrolment"
      enrolmentStoreProxyConnector.getAllocatedEnrolments(utr) map {
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
        case _ => unexpectedResponse(
          apiName = apiName,
          outcomes = outcomes,
          location = location,
          nino = nino
        )
      }
    }
  }

  private def getUserIdsForEnrolment(
    result: SuccessES1,
    utr: String,
    nino: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Failure, SuccessES0] = {
    val apiName = "ES0"
    val outcomes = result.outcomes
    EitherT {
      val location = "getUserIdsForEnrolment"
      enrolmentStoreProxyConnector.getUserIds(utr) map {
        case Right(UsersFound(userIds)) =>
          Right(SuccessES0(
            outcomes = outcomes :+ Outcome.success(apiName),
            userIds = userIds.toSeq
          ))
        case Left(EnrolmentFailure(_, message)) =>
          logError(location, nino, message)
          Left(Failure(
            outcomes = outcomes :+ Outcome.failure(apiName, message)
          ))
        case _ => unexpectedResponse(
          apiName = apiName,
          outcomes = outcomes,
          location = location,
          nino = nino
        )
      }
    }
  }

  private def getAdminUserForGroup(
    result: SuccessES0,
    nino: String,
    groupId: String,
    users: Seq[String]
  )(implicit hc: HeaderCarrier): EitherT[Future, Failure, SuccessUGS] = {
    val apiName = "UGS"
    val outcomes = result.outcomes
    EitherT {
      val location = "getAdminUserForGroup"
      usersGroutSearchConnector.getUsersForGroup(groupId) map {
        case Right(GroupUsersFound(userIds)) =>
          userIds.find(users.contains(_)) match {
            case Some(userId) =>
              Right(SuccessUGS(
                outcomes = outcomes :+ Outcome.success(apiName),
                userId = userId
              ))
            case None =>
              val message = s"No ADMIN users for group: $groupId"
              logError(location, nino, message)
              Left(Failure(
                outcomes = outcomes :+ Outcome.failure(apiName, message)
              ))
          }
        case Left(InvalidJson) =>
          val message = "Invalid JSON in response"
          logError(location, nino, message)
          Left(Failure(
            outcomes = outcomes :+ Outcome.failure(apiName, message)
          ))
        case Left(UsersGroupsSearchConnectionFailure(status)) =>
          val message = s"Response status code: $status"
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

case class SuccessES1(
  outcomes: Seq[Outcome],
  groupId: String
) extends Success

case class SuccessES0(
  outcomes: Seq[Outcome],
  userIds: Seq[String]
) extends Success

case class SuccessUGS(
  outcomes: Seq[Outcome],
  userId: String
) extends Success

case class Failure(
  outcomes: Seq[Outcome] = Seq.empty,
  error: Option[EnrolmentError] = None
)
