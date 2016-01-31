package controllers

import javax.inject.Inject

import models.Country
import models.tables.{ Schedule, ScheduleDao, UserDao }
import play.api.data.Form
import play.api.data.Forms.{ jodaDate, longNumber, mapping, nonEmptyText, number, optional, text }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import play.i18n._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Login(user: String, password: String)

class Admin @Inject() (userDao: UserDao,
    scheduleDao: ScheduleDao, val messagesApi: MessagesApi) extends Controller with I18nSupport with SecuredAdmin {

  val loginForm = Form(
    mapping(
      "user" -> text,
      "password" -> text
    )(Login.apply)(Login.unapply) verifying ("Failed authentication!", fields => fields match {
        case userData => validate(userData.user, userData.password).isDefined
      })
  )

  def validate(user: String, password: String) = {
    val storedUser = play.Play.application.configuration.getString("tippspiel.admin.user")
    val storedPassword = play.Play.application.configuration.getString("tippspiel.admin.password")

    if (user == storedUser && password == storedPassword)
      Some(Login(user, password))
    else
      None
  }

  /**
   * handles authentication form
   *
   * @return
   */
  def login = Action.async { implicit rs =>
    Future.successful(Ok(views.html.admin.login(loginForm)));
  }

  /**
   * handles authentication form
   *
   * @return
   */
  def authenticate = Action.async { implicit rs =>
    loginForm.bindFromRequest.fold(formHasErrors => {
      Future.successful(BadRequest(views.html.admin.login(formHasErrors)));
    },
      login => {
        //set user as auhtenticated
        Future.successful(
          Redirect(routes.Admin.schedulesOfTournament())
            .flashing("success" -> "Authentication Successfull").withSession(Security.username -> login.user));
      })
  }

  val schedulesForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "gameTime" -> jodaDate("dd-MM-yyyy HH:mm"),
      "group" -> nonEmptyText,
      "homeTeam" -> nonEmptyText,
      "visitorTeam" -> nonEmptyText,
      "homeScore" -> optional(number(min = 0, max = 100)),
      "visitorScore" -> optional(number(min = 0, max = 100))
    )(Schedule.apply)(Schedule.unapply)
  )

  val teams: Seq[(String, String)] =
    Country.values.map(x => (x.toString, Messages.get("country." + x.toString))).toSeq

  /**
   *
   * @param scheduleId
   * @return
   */
  def scheduleEdit(scheduleId: Long) = Action.async { implicit rs =>
    val schedule = for {
      t <- scheduleDao.findById(scheduleId)
    } yield t

    schedule.map {
      case (t) =>
        t match {
          case Some(c) => Ok(views.html.admin.scheduleEdit(scheduleId, teams, schedulesForm.fill(c)))
          case None => NotFound
        }
    }
  }

  /**
   * handles schedule edit form changes
   *
   * @param id
   * @return
   */
  def scheduleUpdate(id: Long) = Action.async { implicit rs =>
    schedulesForm.bindFromRequest.fold(formHasErrors => {
      Future.successful(BadRequest(views.html.admin.scheduleEdit(id, teams, formHasErrors)))
    },
      schedule => {
        val x = scheduleDao.update(id, schedule)
        Future.successful(Redirect(routes.Admin.schedulesOfTournament()).flashing("success" -> "Schedule %s - %s has been updated".format(schedule.homeTeam, schedule.visitorTeam)))
      })
  }

  def schedules = Action.async { implicit request =>
    val schedulesList = scheduleDao.list
    schedulesList.map(schedules => {
      println("# schedules = " + schedules.size)
      Ok(views.html.admin.schedulesList(schedules))
    })
  }

  def schedulesOfTournament = Action.async { implicit request =>
    val schedulesList = scheduleDao.list
    schedulesList.map(schedules => {
      println("# schedules = " + schedules.size)
      Ok(views.html.admin.schedulesList(schedules))
    })
  }

  def scheduleCreate() = Action.async { implicit rs =>
    Future.successful(Ok(views.html.admin.scheduleCreate(teams, schedulesForm)))
  }

  def scheduleSave() = Action.async { implicit rs =>
    schedulesForm.bindFromRequest.fold(
      formHasErrors => Future.successful(BadRequest(views.html.admin.scheduleCreate(teams, formHasErrors))),
      schedule => {
        val x = scheduleDao.insert(schedule)
        Future.successful(Redirect(routes.Admin.schedulesOfTournament()).flashing("success" -> "Schedule has been created"))
      })
  }

  /**
   * E-MAIL CONFIRMATION
   */
  // confirm url
  // signupconfirmation?userId=&confirmationToken=?
  def signupConfirmation(userId: Long, confirmationToken: String) = Action.async { implicit request =>
    userDao.findById(userId).flatMap {
      case Some(user) => Future.successful(Ok(views.html.emailConfirmation(user)))
      case None => Future.successful(Ok(views.html.emailConfirmationFailed("Failed")))
    }
  }

}

trait SecuredAdmin {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Admin.login())

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /*
  def withAsyncAuth(f: => String => Request[AnyContent] => Future[Result]): Future[Result] = Action.async {
    Security.Authenticated(username, onUnauthorized) { user =>
      println(user)
    }

    Action(request => f(user)(request))
  }
*/

}
