package controllers

import javax.inject.Inject

import models.SetupDataBase
import models.tables.{ GroupDao, ScheduleDao, UserDao }
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (userDao: UserDao,
    scheduleDao: ScheduleDao, gameDao: GroupDao, val messagesApi: MessagesApi) extends api.ApiController {

  def test = ApiAction { implicit request =>
    ok("The API is ready")
  }

  def realDB = Action.async { implicit request =>
    val userList = userDao.list
    userList.map(users => Ok(views.html.realDB(users)))
  }

  def setupRealDB = Action { implicit request =>
    val usersCreated: Boolean = userDao.schemaCreate()
    val schedulesCreated: Boolean = scheduleDao.testSchemaCreate()
    val gamesCreated: Boolean = gameDao.setup()

    Ok(views.html.setupRealDB(SetupDataBase(usersCreated, schedulesCreated, gamesCreated)))
  }

}