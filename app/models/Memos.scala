package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._
import org.joda.time.{DateTime}

case class Memos(
  id: Int,
  user: Int,
  content: Option[String] = None,
  title: Option[String] = None,
  username: Option[String] = None,
  contentHtml: Option[String] = None,
  isPrivate: Byte,
  createdAt: DateTime,
  updatedAt: DateTime) {

  def save()(implicit session: DBSession = Memos.autoSession): Memos = Memos.save(this)(session)

  def destroy()(implicit session: DBSession = Memos.autoSession): Unit = Memos.destroy(this)(session)

}

object Memos extends SQLSyntaxSupport[Memos] {

  override val tableName = "memos"

  override val columns = Seq("id", "user", "content", "title", "is_private", "created_at", "updated_at")

  def apply(m: ResultName[Memos])(rs: WrappedResultSet): Memos = new Memos(
    id = rs.int(m.id),
    user = rs.int(m.user),
    username = UsersCache.nameOf(rs.int(m.user)),
    content = rs.stringOpt(m.content),
    title   = None,
    isPrivate = rs.byte(m.isPrivate),
    createdAt = rs.timestamp(m.createdAt).toDateTime,
    updatedAt = rs.timestamp(m.updatedAt).toDateTime
  )

  def applyTitleOnly(rs: WrappedResultSet): Memos = new Memos(
    id = rs.int("id"),
    user = rs.int("user"),
    username = UsersCache.nameOf(rs.int("user")),
    content = None,
    title = rs.stringOpt("title"),
    isPrivate = 0,
    createdAt = rs.timestamp("created_at").toDateTime,
    updatedAt = rs.timestamp("updated_at").toDateTime
  )

  val m = Memos.syntax("m")

  val autoSession = AutoSession

  def total(implicit session: DBSession = autoSession): Long = {
    sql"SELECT cnt FROM public_count LIMIT 1".map { rs => rs.long(1) }.single.apply().get
  }

  def public(page:Int = 0)(implicit session: DBSession = autoSession): List[Memos] = {
    sql"""
      SELECT
        m.id,
        m.user,
        m.title,
        m.is_private,
        m.created_at,
        m.updated_at
      FROM memos AS m
      JOIN public_memos AS pm
        ON pm.memo = m.id AND
           pm.id BETWEEN ((SELECT cnt FROM public_count LIMIT 1) - ${(page + 1) * 100 - 1}) AND ((SELECT cnt FROM public_count LIMIT 1) - ${page * 100})
      ORDER BY m.id DESC
    """.map(Memos.applyTitleOnly).list.apply()
  }

  def findByUser(user_id:Int)(implicit session: DBSession = autoSession):List[Memos] = {
    withSQL {
      select.from(Memos as m).where.eq(m.user, user_id).append(sqls"ORDER BY id")
    }.map(Memos(m.resultName)).list.apply()
  }

  def findPublicByUser(user_id:Int)(implicit session: DBSession = autoSession):List[Memos] = {
    withSQL {
      select.from(Memos as m).where.eq(m.user, user_id).and.eq(m.isPrivate, 0).append(sqls"ORDER BY id DESC")
    }.map(Memos(m.resultName)).list.apply()
  }

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Memos] = {
    withSQL {
      select.from(Memos as m).where.eq(m.id, id)
    }.map(Memos(m.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Memos] = {
    withSQL(select.from(Memos as m)).map(Memos(m.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Memos as m)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Memos] = {
    withSQL {
      select.from(Memos as m).where.append(sqls"${where}")
    }.map(Memos(m.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Memos as m).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    user: Int,
    content: Option[String] = None,
    title: Option[String] = None,
    isPrivate: Byte,
    createdAt: DateTime,
    updatedAt: DateTime)(implicit session: DBSession = autoSession): Memos = {
    val generatedKey = withSQL {
      insert.into(Memos).columns(
        column.user,
        column.content,
        column.title,
        column.isPrivate,
        column.createdAt,
        column.updatedAt
      ).values(
        user,
        content,
        title,
        isPrivate,
        createdAt,
        updatedAt
      )
    }.updateAndReturnGeneratedKey.apply()

    Memos(
      id        = generatedKey.toInt,
      user      = user,
      content   = content,
      title     = title,
      isPrivate = isPrivate,
      createdAt = createdAt,
      updatedAt = updatedAt)
  }

  def save(entity: Memos)(implicit session: DBSession = autoSession): Memos = {
    withSQL {
      update(Memos as m).set(
        m.id        -> entity.id,
        m.user      -> entity.user,
        m.content   -> entity.content,
        m.title     -> entity.content.flatMap(_.split("""\r?\n""").headOption),
        m.isPrivate -> entity.isPrivate,
        m.createdAt -> entity.createdAt,
        m.updatedAt -> entity.updatedAt
      ).where.eq(m.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: Memos)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(Memos).where.eq(column.id, entity.id) }.update.apply()
  }

  def insertPublicMemos(memoId:Int)(implicit session: DBSession = autoSession):Unit = {
    sql"INSERT INTO public_memos(memo) VALUES(${memoId})".update.apply()
  }

  def incrementCount(implicit session: DBSession = autoSession): Unit =  {
    sql"UPDATE public_count SET cnt = cnt + 1".update.apply()
  }

  def newer(memo:Memos, public:Boolean)(implicit session: DBSession = autoSession):Option[Int] = {
    (if(public) {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id > ${memo.id} AND is_private=0 ORDER BY ID LIMIT 1"
    } else {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id > ${memo.id} ORDER BY ID LIMIT 1"
    }).map { rs => rs.int(1) }.single.apply()
  }

  def older(memo:Memos, public:Boolean)(implicit session: DBSession = autoSession):Option[Int] = {
    (if(public) {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id < ${memo.id} AND is_private=0 ORDER BY ID LIMIT 1"
    } else {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id < ${memo.id} ORDER BY ID LIMIT 1"
    }).map { rs => rs.int(1) }.single.apply()
  }

}
