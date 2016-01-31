package models

import java.text.SimpleDateFormat
import javax.xml.datatype.DatatypeConstants

import api.Page
import models.tables.{ Schedule, Group }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.Try

/*
* A fake DB to store and load all the data
*/
object FakeDB {

  private val dtf = DateTimeFormat.forPattern("yyyy-mm-dd hh:mm:ss")

  val dt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss")

  // API KEYS
  val apiKeys = FakeTable(
    1L -> ApiKey(apiKey = "AbCdEfGhIjK1", name = "ios-app", active = true),
    2L -> ApiKey(apiKey = "AbCdEfGhIjK2", name = "android-app", active = true)
  )

  // TOKENS
  val tokens = FakeTable[ApiToken]()

  // API REQUEST LOG
  val logs = FakeTable[ApiLog]()

  // SCHEDULES
  val aDate = Try(dt.parse("2015-09-06 10:11:00"))
    .map(d => new java.sql.Date(d.getTime()))
    .toOption

  val schedules = FakeTable(
    1L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "A", "Frankreich", "Rumänien", Some(0), Some(1)),
    2L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "A", "Frankreich", "Rumänien", Some(0), Some(1)),
    3L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "A", "Frankreich", "Rumänien", Some(0), Some(1)),
    4L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "A", "Frankreich", "Rumänien", Some(0), Some(1)),
    5L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "B", "Frankreich", "Rumänien", Some(0), Some(1)),
    6L -> Schedule(None, DateTime.parse("2015-09-06 10:11:00", dtf),
      "B", "Frankreich", "Rumänien", Some(0), Some(1))
  )

  // GAMES
  val games = FakeTable(
    1L -> Group(None, 1L, "Game Name 1", Some("Beschreibung1")),
    2L -> Group(None, 1L, "Game Name 2", None),
    3L -> Group(None, 2L, "Game Name 3", None)
  )

  /*
	* Fake table that emulates a SQL table with an auto-increment index
	*/
  case class FakeTable[A](var table: Map[Long, A], var incr: Long) {
    def nextId: Long = {
      if (!table.contains(incr))
        incr
      else {
        incr += 1
        nextId
      }
    }

    def get(id: Long): Option[A] = table.get(id)

    def find(p: A => Boolean): Option[A] = table.values.find(p)

    def insert(a: Long => A): (Long, A) = {
      val id = nextId
      val tuple = (id -> a(id))
      table += tuple
      incr += 1
      tuple
    }

    def update(id: Long)(f: A => A): Boolean = {
      get(id).map { a =>
        table += (id -> f(a))
        true
      }.getOrElse(false)
    }

    def delete(id: Long): Unit = table -= id

    def delete(p: A => Boolean): Unit = table = table.filterNot { case (id, a) => p(a) }

    def values: List[A] = table.values.toList

    def map[B](f: A => B): List[B] = values.map(f)

    def filter(p: A => Boolean): List[A] = values.filter(p)

    def exists(p: A => Boolean): Boolean = values.exists(p)

    def count(p: A => Boolean): Int = values.count(p)

    def size: Int = table.size

    def isEmpty: Boolean = size == 0

    def page(p: Int, s: Int)(filterFunc: A => Boolean)(sortFuncs: ((A, A) => Boolean)*): Page[A] = {
      val items = filter(filterFunc)
      val sorted = sortFuncs.foldRight(items)((f, items) => items.sortWith(f))
      Page(
        items = sorted.drop((p - 1) * s).take(s),
        page = p,
        size = s,
        total = sorted.size
      )
    }
  }

  object FakeTable {
    def apply[A](elements: (Long, A)*): FakeTable[A] = apply(Map(elements: _*), elements.size + 1)
  }

}

