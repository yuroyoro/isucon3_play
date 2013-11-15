package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import org.joda.time.{DateTime}

case class Memos(
  id: Int,
  user: Int,
  content: Option[String] = None,
  username: Option[String] = None,
  contentHtml: Option[String] = None,
  isPrivate: Byte,
  createdAt: DateTime,
  updatedAt: DateTime) {
}

object Memos {
  import models.AnormExtension._

  def parse(row:SqlRow):Memos = Memos(
    row[Int]("id"),
    row[Int]("user"),
    row[Option[String]]("content"),
    None,
    None,
    row[Int]("is_private").toByte,
    row[DateTime]("created_at"),
    row[DateTime]("updated_at")
  )

  def total: Long = DB.withConnection {
    implicit c => SQL("select count(1) as c from memos where is_private=0").as(scalar[Long].single)
  }

  def public(page:Int = 0): List[Memos] = DB.withConnection {
    implicit c => SQL(s"select m.* from memos as m where is_private=0 order by created_at desc, id desc limit 100 offset ${page * 100}")().map(parse).toList
  }

  def findByUser(user_id:Int): List[Memos] = DB.withConnection {
    implicit c => SQL(s"select m.* from memos as m where m.user=${user_id} order by created_at desc")().map(parse).toList
  }

  def findPublicByUser(user_id:Int): List[Memos] = DB.withConnection {
    implicit c => SQL(s"select m.* from memos as m where m.is_private=0 and m.user=${user_id} order by created_at desc")().map(parse).toList
  }

  def find(id: Int): Option[Memos] = DB.withConnection {
    implicit c => SQL(s"select m.* from memos as m where m.id=${id} limit 1")().map(parse).headOption
  }

  def create(
    user: Int,
    content: Option[String] = None,
    isPrivate: Byte,
    createdAt: DateTime,
    updatedAt: DateTime): Memos = DB.withConnection {
      implicit c =>
        val id = SQL("insert into memos(user, content, is_private, created_at, updated_at) values ({user}, {content}, {is_private}, {created_at}, {updated_at})").on(
          "user"       -> user,
          "content"    -> content,
          "is_private" -> isPrivate,
          "created_at" -> createdAt,
          "updated_at" -> updatedAt).executeInsert(scalar[Long].single).toInt

      Memos(
        id        = id,
        user      = user,
        content   = content,
        isPrivate = isPrivate,
        createdAt = createdAt,
        updatedAt = updatedAt)
  }
}
