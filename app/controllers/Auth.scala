package controllers

import javax.inject.Inject

import api.ApiError._
import api.JsonCombinators._
import models.tables.UserDao
import models.{ ApiToken, User }
import play.api.Play.current
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Akka
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class Auth @Inject() (userDao: UserDao, mailerClient: MailerClient, val messagesApi: MessagesApi) extends api.ApiController {

  implicit val loginInfoReads: Reads[Tuple2[String, String]] = (
    (__ \ "email").read[String](Reads.email) and
      (__ \ "password").read[String] tupled
  )

  def signIn = ApiActionWithBody { implicit request =>
    readFromRequest[Tuple2[String, String]] {
      case (email, pwd) =>
        userDao.findByEmail(email).flatMap {
          case None => errorUserNotFound
          case Some(user) => {
            if (user.password != pwd) errorUserNotFound
            else if (!user.emailConfirmed) errorUserEmailUnconfirmed
            else ApiToken.create(request.apiKeyOpt.get, user.id.getOrElse(0L)).flatMap { token =>
              ok(Json.obj(
                "token" -> token,
                "minutes" -> 10
              ))
            }
          }
        }
    }
  }

  def signOut = SecuredApiAction { implicit request =>
    ApiToken.delete(request.token).flatMap { _ =>
      noContent()
    }
  }

  implicit val signUpInfoReads: Reads[Tuple3[String, String, User]] = (
    (__ \ "email").read[String](Reads.email) and
      (__ \ "password").read[String](Reads.minLength[String](6)) and
      (__ \ "user").read[User] tupled
  )

  def signUp = ApiActionWithBody { implicit request =>
    readFromRequest[Tuple3[String, String, User]] {
      case (email, password, user) =>
        userDao.findByEmail(email).flatMap {
          case Some(anotherUser) => errorCustom("api.error.signup.email.exists")
          case None =>
            userDao.insert(email, password, user.name)
            userDao.findByEmail(email).flatMap {
              case Some(user) => {

                sendConfirmationEmail(user);

                ok(user)
              }
              case None => errorCustom("api.error.signup.email.exists")
            }
        }
    }
  }

  private def sendConfirmationEmail(user: User) {
    val cid = "1234"
    val confirmationUrl = "http://localhost:9000/signupconfirmation?userId=" + user.id + "&confirmationToken="
    val email = Email(
      "Bestätigungs E-Mail",
      "Mister FROM <cyrill.zadra@tie.ch>",
      Seq("Miss TO <cyrill.zadra@gmail.com>"),
      // adds attachment
      //attachments = Seq(
      //AttachmentFile("attachment.pdf", new File("/some/path/attachment.pdf")),
      // adds inline attachment from byte array
      //AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE)),
      // adds cid attachment
      //AttachmentFile("image.jpg", new File("/some/path/image.jpg"), contentId = Some(cid))
      //),
      // sends text, HTML or both...
      bodyText = Some("A text message"),
      bodyHtml = Some(s"""<html><body><p>An <b>html</b> message with cid <img src="cid:$cid"></p><a href="$confirmationUrl">$confirmationUrl</a></body></html>""")
    )
    mailerClient.send(email)
  }

}