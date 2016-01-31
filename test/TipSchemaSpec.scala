import models.User
import models.tables.{ScheduleDao, Tip, TipDao, UserDao}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterAll
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{WithApplicationLoader, PlaySpecification}
import play.api.{Application, Configuration}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by tiezad on 20.01.2016.
  */
@RunWith(classOf[JUnitRunner])
class TipSchemaSpec extends PlaySpecification with BeforeAfterAll {

  override def beforeAll(): Unit = {
    val daos = getDaos
    val tipDao: TipDao = daos._1
    tipDao.testSchemaCreate()
    val scheduleDao: ScheduleDao = daos._2
    scheduleDao.testSchemaCreate()
  }

  override def afterAll(): Unit = {
    val daos = getDaos
    val tipDao: TipDao = daos._1
    tipDao.testSchemaDrop()
    val scheduleDao: ScheduleDao = daos._2
    scheduleDao.testSchemaDrop()
  }


  "TIPDAO" should {
    val app2dao = Application.instanceCache[TipDao]

    "find user by userId && groupId" in new WithApplicationLoader {
      val tipDao: TipDao = app2dao(app)

      val tips : Seq[Tip] = Await.result(tipDao.findById(1,1), 2 seconds)

      tips.length must equalTo(3)
    }

    "count tips" in new WithApplicationLoader {
      val tipDao: TipDao = app2dao(app)

      val numberOfTips = Await.result(tipDao.count(), 2 seconds)

      numberOfTips must equalTo(6)
    }

    "new tips for user x in group y" in new WithApplicationLoader {
      val tipDao: TipDao = app2dao(app)

      tipDao.insertTips(3,1)

      val numberOfTips = Await.result(tipDao.count(), 2 seconds)

      numberOfTips must equalTo(10)
    }

  }

  private def getDaos: (TipDao,ScheduleDao) = {
    val application = new GuiceApplicationBuilder()
      .loadConfig(env => Configuration.load(env))
      .build
    val app2tipDao = Application.instanceCache[TipDao]
    val app2scheduleDao = Application.instanceCache[ScheduleDao]
    val tipDao: TipDao = app2tipDao(application)
    val scheduleDao: ScheduleDao = app2scheduleDao(application)

    (tipDao,scheduleDao)
  }

}
