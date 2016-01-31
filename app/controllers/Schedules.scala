package controllers

import javax.inject.Inject

import models.tables.ScheduleDao
import play.api.i18n.MessagesApi
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global

class Schedules @Inject() (scheduleDao: ScheduleDao, val messagesApi: MessagesApi) extends api.ApiController {

  def list(tournamentId: Long) = SecuredApiAction { implicit request =>
    val x = scheduleDao.list
    x.flatMap { list =>
      ok(list.map(g => Json.obj("id" -> g.id, "group" -> g.group)))
    }
  }

}