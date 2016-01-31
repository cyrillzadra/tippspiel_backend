package controllers

import javax.inject.Inject

import api.ApiError._
import api.JsonCombinators._
import models.tables.{ GroupDao, UserDao }
import models.{ ApiToken, User }
import play.api.i18n.MessagesApi
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global

class Account @Inject() (gameDao: GroupDao, userDao: UserDao, val messagesApi: MessagesApi) extends api.ApiController {

  def info = SecuredApiAction { implicit request =>
    maybeItem(userDao.findById(request.userId))
  }

  def myGames = SecuredApiAction { implicit request =>
    val x = gameDao.listByCreatorId(request.userId)
    x.flatMap { list =>
      ok(list.map(g => Json.obj("id" -> g.id, "creatorId" -> g.creatorId, "name" -> g.name)))
    }
  }

  def update = SecuredApiActionWithBody { implicit request =>
    readFromRequest[User] { user =>
      userDao.update(request.userId, user).flatMap { isOk =>
        //TODO
        //if (isOk) noContent() else errorInternal
        noContent()
      }
    }
  }

  implicit val pwdsReads: Reads[Tuple2[String, String]] = (
    (__ \ "old").read[String](Reads.minLength[String](1)) and
      (__ \ "new").read[String](Reads.minLength[String](6)) tupled
  )

  def updatePassword = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Tuple2[String, String]] {
      case (oldPwd, newPwd) =>
        userDao.findById(request.userId).flatMap {
          case None => errorUserNotFound
          case Some(user) if (oldPwd != user.password) => errorCustom("api.error.reset.pwd.old.incorrect")
          case Some(user) => userDao.updatePassword(request.userId, newPwd).flatMap { isOk =>
            //TODO
            //if (isOk) noContent() else errorInternal
            noContent()
          }
        }
    }
  }

  def delete = SecuredApiAction { implicit request =>
    ApiToken.delete(request.token).flatMap { _ =>
      userDao.delete(request.userId).flatMap { _ =>
        noContent()
      }
    }
  }

}