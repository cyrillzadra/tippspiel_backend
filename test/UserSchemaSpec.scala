import models.User
import models.tables.UserDao
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{BeforeAfterAll, AfterAll, BeforeSpec, BeforeAll}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Application}
import play.api.test.{PlaySpecification, WithApplicationLoader}
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by tiezad on 20.01.2016.
  */
@RunWith(classOf[JUnitRunner])
class UserSchemaSpec extends PlaySpecification with BeforeAfterAll {

  override def beforeAll(): Unit = {
    val userDao: UserDao = getUserDao
    userDao.schemaCreate()
  }

  override def afterAll(): Unit = {
    val userDao: UserDao = getUserDao
    userDao.schemaDrop()
  }


  "USERDAO" should {
    val app2dao = Application.instanceCache[UserDao]

    "find user by id" in new WithApplicationLoader {
      val userDao: UserDao = app2dao(app)

      val user = Await.result(userDao.findById(1), 2 seconds)

      user must beSome
      user.map { user =>
        user.name must equalTo("User 1")
      }
    }

    "count user" in new WithApplicationLoader {
      val userDao: UserDao = app2dao(app)

      val numberOfUsers = Await.result(userDao.count(), 2 seconds)

      numberOfUsers must equalTo(3)
    }

    "insert user" in new WithApplicationLoader {
      val userDao: UserDao = app2dao(app)

      val newUser: User = User(None, "user4@mail.com", "123456", "User 3", "DE", true)

      userDao.insert(newUser)

      val user = Await.result(userDao.findByEmail("user4@mail.com"), 2 seconds)

      user must beSome
      user.map { user =>
        user.name must equalTo(newUser.name)
        user.country must equalTo(newUser.country)
        user.password must equalTo(newUser.password)
      }
    }

  }


  private def getUserDao: UserDao = {
    val application = new GuiceApplicationBuilder()
      .loadConfig(env => Configuration.load(env))
      .build
    val app2dao = Application.instanceCache[UserDao]
    val userDao: UserDao = app2dao(application)
    userDao
  }

}
