package controllers

import javax.inject.Inject

import models.tables.UserDao
import play.api.i18n.MessagesApi
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global

class Users @Inject() (userDao: UserDao, val messagesApi: MessagesApi) extends api.ApiController {

  def usernames = ApiAction { implicit request =>
    userDao.list.flatMap { list =>
      ok(list.map(u => Json.obj("id" -> u.id, "name" -> u.name, "email" -> u.email)))
    }
  }

}