package models

import play.api.db._
import play.api.Play.current
import anorm._
import org.joda.time.{DateTime}

case class Users(
  id: Int,
  username: String,
  password: String,
  salt: String,
  lastAccess: Option[DateTime] = None) {

  def updateLastAccess(): Unit = Users.updateLastAccess(this.id)
}

object Users {
  import models.AnormExtension._

  def parse(row:SqlRow):Users = Users(
    row[Int]("id"),
    row[String]("username"),
    row[String]("password"),
    row[String]("salt"),
    row[Option[DateTime]]("last_access")(Column.rowToOption(rowToDateTime))
  )

  def findByName(username:String):Option[Users] = DB.withConnection {
    implicit c =>  SQL(s"select * from users where username='${username}' limit 1")().map(parse).headOption
  }

  def nameOf(id:Int):Option[String] = DB.withConnection {
    implicit c =>  SQL(s"select username from users where id=${id} limit 1")().map(row => row[String]("username")).headOption
  }

  def find(id:Int):Option[Users] = DB.withConnection {
    implicit c =>  SQL(s"select * from users where id=${id} limit 1")().map(parse).headOption
  }

  def updateLastAccess(id:Int): Unit = DB.withConnection {
    implicit c => SQL("update users set last_access=now() where id={id}").on("id" -> id).executeUpdate()
  }
}

