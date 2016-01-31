package models.tables

import java.sql.Timestamp

import models.User
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

/**
 * Created by tiezad on 20.01.2016.
 */
trait Schema {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class SchedulesT(tag: Tag) extends Table[Schedule](tag, "SCHEDULE") {

    implicit val timestamp2dateTime = MappedColumnType.base[DateTime, Timestamp](
      dateTime => new Timestamp(dateTime.getMillis),
      timestamp => new DateTime(timestamp)
    )

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def gameTime = column[DateTime]("GAME_TIME")

    def group = column[String]("GROUP")

    def homeTeam = column[String]("HOME_TEAM")

    def visitorTeam = column[String]("VISITOR_TEAM")

    def homeScore = column[Option[Int]]("HOME_SCORE")

    def visitorScore = column[Option[Int]]("VISITOR_SCORE")

    def * = (id.?, gameTime, group, homeTeam, visitorTeam, homeScore, visitorScore) <> (Schedule.tupled, Schedule.unapply _)

  }

  val users = TableQuery[UserT];

  class GroupT(tag: Tag) extends Table[Group](tag, "GROUP") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def creatorId = column[Long]("CREATOR_ID")

    //TODO uniqueness of group
    def name = column[String]("NAME")

    def description = column[Option[String]]("DESCRIPTION")

    def * = (id.?, creatorId, name, description) <> (Group.tupled, Group.unapply _)

    def creatorFk = foreignKey("creator_fk", creatorId, users)(_.id)

    def creatorJoin = users.filter(_.id === creatorId)

  }

  class TipT(tag: Tag) extends Table[Tip](tag, "USER_TIP") {

    def userId = column[Long]("USER_ID");

    def groupId = column[Long]("GROUP_ID");

    def scheduleId = column[Long]("SCHEDULE_ID")

    def homeScore = column[Option[Int]]("HOME_SCORE")

    def visitorScore = column[Option[Int]]("VISITOR_SCORE")

    def * = (groupId, scheduleId, userId, homeScore, visitorScore) <> (Tip.tupled, Tip.unapply _)

    def pk = primaryKey("PK_USER_TIP", (userId, groupId, scheduleId))

    def userFk = foreignKey("user_fk", userId, TableQuery[UserT])(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def groupFk = foreignKey("group_fk", groupId, TableQuery[GroupT])(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def scheduleFk = foreignKey("schedule_fk", groupId, TableQuery[SchedulesT])(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def idxUserGroup = index("idx_user_group", (userId, groupId), unique = false)

  }

  class UserT(tag: Tag) extends Table[User](tag, "USER") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def email = column[String]("EMAIL")

    def password = column[String]("PASSWORD")

    def name = column[String]("NAME")

    def country = column[String]("COUNTRY")

    def emailConfirmed = column[Boolean]("EMAIL_CONFIRMED")

    def idx = index("unique_email", (email), unique = true)

    def * = (id.?, email, password, name, country, emailConfirmed) <> (User.tupled, User.unapply _)
  }

}