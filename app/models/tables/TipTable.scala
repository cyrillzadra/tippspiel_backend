package models.tables

/**
 * Created by tiezad on 29.12.2015.
 */

import javax.inject.{ Inject, Singleton }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

case class Tip(
  userId: Long,
  groupId: Long,
  scheduleId: Long,
  homeScore: Option[Int] = None,
  visitorScore: Option[Int] = None)

@Singleton()
class TipDao @Inject() (scheduleDao: ScheduleDao, protected val dbConfigProvider: DatabaseConfigProvider) extends Schema
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val tips = TableQuery[TipT]

  /** Retrieve a user by id. */
  def findById(userId: Long, groupId: Long): Future[Seq[Tip]] =
    db.run(tips.filter(t => (t.userId === userId && t.groupId === groupId)).result)

  /** Insert a new tip */
  def insert(tip: Tip): Future[Unit] =
    db.run(tips += tip).map(_ => ())

  /** Insert new tips */
  def insert(tips: Seq[Tip]): Future[Unit] =
    db.run(this.tips ++= tips).map(_ => ())

  def insertTips(userId: Int, groupId: Int): Future[Unit] = {
    //TODO kann
    val schedules = Await.result(scheduleDao.list, 10 seconds)
    val newTips = for {
      schedule <- schedules
    } yield Tip(userId, groupId, schedule.id.getOrElse(0))

    db.run(this.tips ++= newTips).map(_ => ())
  }

  /** Update a tip. */
  def update(tip: Tip): Future[Unit] = {
    db.run(tips.filter(t => (t.userId === tip.userId && t.groupId === tip.groupId && t.scheduleId === tip.scheduleId))
      .map(col => (col.homeScore, col.visitorScore))
      .update(tip.homeScore, tip.visitorScore).map(_ => ()))
  }

  /** Count all users. */
  def count(): Future[Int] =
    db.run(tips.map(_.userId).length.result)

  /** Return a list of (Tip) */
  def list: Future[Seq[Tip]] =
    db.run(tips.result)

  def testSchemaCreate(): Boolean = {
    tips.schema.create.statements.foreach(println)

    dbConfig.db.run(DBIO.seq(
      tips.schema.create,

      // Insert some Groups
      tips += Tip(1, 1, 1, None, None),
      tips += Tip(1, 1, 2, None, None),
      tips += Tip(1, 1, 3, None, None),
      tips += Tip(1, 2, 1, None, None),
      tips += Tip(1, 2, 2, None, None),
      tips += Tip(1, 2, 3, None, None)

    ))

    true
  }

  def testSchemaDrop(): Boolean = {
    dbConfig.db.run(DBIO.seq(
      tips.schema.drop
    ))
    true
  }
}