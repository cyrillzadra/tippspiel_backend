package models.tables

/**
 * Created by tiezad on 29.12.2015.
 */

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.{ Inject, Singleton }

import models.Country
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Schedule(
  id: Option[Long],
  gameTime: DateTime,
  group: String,
  homeTeam: String,
  visitorTeam: String,
  homeScore: Option[Int] = None,
  visitorScore: Option[Int] = None)

@Singleton()
class ScheduleDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends Schema
    with HasDatabaseConfigProvider[JdbcProfile] {

  val dt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss")

  import driver.api._

  val schedules = TableQuery[SchedulesT]

  /** Retrieve a Schedule by id. */
  def findById(id: Long): Future[Option[Schedule]] =
    db.run(schedules.filter(_.id === id).result.headOption)

  /** Insert a new Schedule */
  def insert(schedule: Schedule): Future[Unit] =
    db.run(schedules += schedule).map(_ => ())

  /** Insert new Schedules */
  def insert(schedules: Seq[Schedule]): Future[Unit] =
    db.run(this.schedules ++= schedules).map(_ => ())

  /** Update a Schedule. */
  def update(id: Long, schedule: Schedule): Future[Unit] = {
    val scheduleToUpdate: Schedule = schedule.copy(Some(id))
    db.run(schedules.filter(_.id === id).update(scheduleToUpdate)).map(_ => ())
  }

  /** Return a list of all Schedule */
  def list: Future[Seq[Schedule]] = {
    val listStmt = schedules.result
    println(listStmt.statements)
    db.run(listStmt)
  }

  /** Delete a Schedule. */
  def delete(id: Long): Future[Unit] =
    db.run(schedules.filter(_.id === id).delete).map(_ => ())

  def testSchemaCreate(): Boolean = {
    val dtf = DateTimeFormat.forPattern("yyyy-mm-dd hh:mm:ss")

    schedules.schema.create.statements.foreach(println)

    dbConfig.db.run(DBIO.seq(
      schedules.schema.create,

      // Insert some schedules
      schedules += Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
        "A", Country.FR.toString, Country.RO.toString, Some(0), Some(1)),
      schedules += Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf), "A", Country.AL.toString, Country.CH.toString, None, None),
      schedules += Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf), "A", Country.RO.toString, Country.CH.toString, None, None),
      schedules += Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf), "B", Country.GB.toString, Country.RU.toString, None, None)
    ))

    println(schedules.insertStatement)

    true
  }

  def testSchemaDrop(): Boolean = {
    dbConfig.db.run(DBIO.seq(
      schedules.schema.create
    ))
    true
  }
}