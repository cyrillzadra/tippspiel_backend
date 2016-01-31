package models.tables

/**
 * Created by tiezad on 29.12.2015.
 */

import javax.inject.{ Inject, Singleton }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Group(
  id: Option[Long],
  creatorId: Long,
  name: String,
  description: Option[String])

@Singleton()
class GroupDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends Schema
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val groups = TableQuery[GroupT]

  /** Retrieve a group by id. */
  def findById(id: Long): Future[Option[Group]] =
    db.run(groups.filter(_.id === id).result.headOption)

  /** Insert a new group */
  def insert(group: Group): Future[Unit] =
    db.run(groups += group).map(_ => ())

  /** Insert new groups */
  def insert(groups: Seq[Group]): Future[Unit] =
    db.run(this.groups ++= groups).map(_ => ())

  /** Update a Group. */
  def update(id: Long, group: Group): Future[Unit] = {
    val groupToUpdate: Group = group.copy(Some(id))
    db.run(groups.filter(_.id === id).update(groupToUpdate)).map(_ => ())
  }

  /** Return a list of (Group) */
  def list: Future[Seq[Group]] =
    db.run(groups.result)

  /** Return a list of (Group) by Creator Id */
  def listByCreatorId(userId: Long): Future[Seq[Group]] =
    db.run(groups.filter(_.creatorId === userId).result)

  /** Delete a Group. */
  def delete(id: Long): Future[Unit] =
    db.run(groups.filter(_.id === id).delete).map(_ => ())

  def setup(): Boolean = {
    groups.schema.create.statements.foreach(println)

    dbConfig.db.run(DBIO.seq(
      groups.schema.create,

      // Insert some Groups
      groups += Group(None, 1, "groupName1", Some("Beschreibung 1")),
      groups += Group(None, 1, "groupName2", Some("Beschreibung 2")),
      groups += Group(None, 1, "groupName3", Some("Beschreibung 3"))
    ))

    true
  }
}