package models.tables

/**
 * Created by tiezad on 29.12.2015.
 */

import javax.inject.{ Inject, Singleton }

import models.User
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton()
class UserDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends Schema
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  /** Retrieve a user by id. */
  def findById(id: Long): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  /** Retrieve a user by E-Mail. */
  def findByEmail(email: String): Future[Option[User]] =
    db.run(users.filter(_.email === email).result.headOption)

  /** Insert a new user */
  def insert(user: User): Future[Unit] =
    db.run(users += user).map(_ => ())

  /** Insert new users */
  def insert(users: Seq[User]): Future[Unit] =
    db.run(this.users ++= users).map(_ => ())

  def insert(email: String, password: String, name: String): Future[Unit] = Future.successful {
    db.run(users += User(None, email, password, name, country = "CH", false))
      .map(_ => ())
  }

  /** Update a User. */
  def update(id: Long, user: User): Future[Unit] = {
    val userToUpdate: User = user.copy(Some(id))
    db.run(users.filter(_.id === id).update(userToUpdate)).map(_ => ())
  }

  /** Update a User. */
  def updatePassword(id: Long, password: String): Future[Unit] = {
    db.run(users.filter(_.id === id)
      .map(col => col.password)
      .update(password).map(_ => ()))
  }

  /** Return a list of (User) */
  def list: Future[Seq[User]] =
    db.run(users.result)

  /** Count all users. */
  def count(): Future[Int] =
    db.run(users.map(_.id).length.result)

  /** Delete a User. */
  def delete(id: Long): Future[Unit] =
    db.run(users.filter(_.id === id).delete).map(_ => ())

  def confirmEmail(id: Long): Future[Unit] = Future.successful {
    db.run(users.filter(_.id === id)
      .map(col => (col.emailConfirmed))
      .update(true)).map(_ => ())
  }

  def schemaCreate(): Boolean = {
    users.schema.create.statements.foreach(println)

    dbConfig.db.run(DBIO.seq(
      users.schema.create,

      // Insert some users
      users += User(None, "user1@mail.com", "123456", "User 1", "CH", true),
      users += User(None, "user2@mail.com", "123456", "User 2", "CH", true),
      users += User(None, "user3@mail.com", "123456", "User 3", "DE", true)

    ))

    true
  }

  def schemaDrop(): Boolean = {
    dbConfig.db.run(DBIO.seq(
      users.schema.drop
    ))
    true
  }
}