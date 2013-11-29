package models

import scalikejdbc._, async._, FutureImplicits._, SQLInterpolation._
import scala.concurrent._
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
  updatedAt: DateTime) extends ShortenedNames{

  def save()(implicit session: AsyncDBSession = AsyncDB.sharedSession,  cxt: EC = ECGlobal): Future[Memos] = Memos.save(this)(session, cxt)

  def destroy()(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Int] = Memos.destroy(this)(session, cxt)

}

object Memos extends SQLSyntaxSupport[Memos] with ShortenedNames{

  override val tableName = "memos"

  override val columns = Seq("id", "user", "content", "title", "is_private", "created_at", "updated_at")

  def apply(m: ResultName[Memos])(rs: WrappedResultSet): Memos = new Memos(

    id = rs.int(m.id),
    user = rs.int(m.user),
    username = UsersCache.nameOf(rs.int(m.user)),
    content = Option(rs.string(m.content)),
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
    title = Option(rs.string("title")),
    isPrivate = 0,
    createdAt = rs.timestamp("created_at").toDateTime,
    updatedAt = rs.timestamp("updated_at").toDateTime
  )

  val m = Memos.syntax("m")

  val autoSession = AutoSession

  def total(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    sql"SELECT cnt FROM public_count LIMIT 1".map { rs => rs.long(1) }.single.future.map(_.get)
  }

  def public_memos_total(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    sql"SELECT count(1) FROM public_memos".map { rs => rs.long(1) }.single.future.map(_.get)
  }

  def public(total:Long, page:Int = 0)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[List[Memos]] = {
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
           pm.id BETWEEN ${total - ((page + 1) * 100 - 1)} AND ${total - (page * 100)}
      ORDER BY m.id DESC
    """.map(Memos.applyTitleOnly).list.future
  }

  def findByUser(user_id:Int)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[List[Memos]] = {
    withSQL {
      select.from(Memos as m).where.eq(m.user, user_id).append(sqls"ORDER BY id")
    }.map(Memos(m.resultName)).list.future
  }

  def findPublicByUser(user_id:Int)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[List[Memos]] = {
    withSQL {
      select.from(Memos as m).where.eq(m.user, user_id).and.eq(m.isPrivate, 0).append(sqls"ORDER BY id DESC")
    }.map(Memos(m.resultName)).list.future
  }

  def find(id: Int)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Option[Memos]] = {
    withSQL {
      select.from(Memos as m).where.eq(m.id, id)
    }.map(Memos(m.resultName)).single.future
  }

  def findAll()(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[List[Memos]] = {
    withSQL(select.from(Memos as m)).map(Memos(m.resultName)).list.future
  }

  def countAll()(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    withSQL(select(sqls"count(1)").from(Memos as m)).map(rs => rs.long(1)).single.future.map(_.get)
  }

  def findAllBy(where: SQLSyntax)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[List[Memos]] = {
    withSQL {
      select.from(Memos as m).where.append(sqls"${where}")
    }.map(Memos(m.resultName)).list.future
  }

  def countBy(where: SQLSyntax)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    withSQL {
      select(sqls"count(1)").from(Memos as m).where.append(sqls"${where}")
    }.map(_.long(1)).single.future.map(_.get)
  }

  def create(
    user: Int,
    content: Option[String] = None,
    title: Option[String] = None,
    isPrivate: Byte,
    createdAt: DateTime,
    updatedAt: DateTime)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Memos] = {
    for{
      id <- withSQL {
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
      }.updateAndReturnGeneratedKey()
    } yield {
      Memos(
        id        = id.toInt,
        user      = user,
        content   = content,
        title     = title,
        isPrivate = isPrivate,
        createdAt = createdAt,
        updatedAt = updatedAt)
    }
  }

  def save(entity: Memos)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Memos] = {
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
    }.update.future.map{ _ => entity }
  }

  def destroy(entity: Memos)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Int] = {
    withSQL { delete.from(Memos).where.eq(column.id, entity.id) }.update.future
  }

  def insertPublicMemos(memo:Memos)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[Int] = {
    if(memo.isPrivate == 0) {
      sql"INSERT INTO public_memos(memo) VALUES(${memo.id})".update.future
    } else {
      Future.successful(0)
    }
  }

  def incrementCount(memo:Memos)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Int] =  {
    if(memo.isPrivate == 0) {
      sql"UPDATE public_count SET cnt = cnt + 1".update.future
    } else {
      Future.successful(0)
    }
  }

  def newer(memo:Memos, public:Boolean)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[Option[Int]] = {
    (if(public) {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id > ${memo.id} AND is_private=0 ORDER BY ID LIMIT 1"
    } else {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id > ${memo.id} ORDER BY ID LIMIT 1"
    }).map { rs => rs.int(1) }.single.future
  }

  def older(memo:Memos, public:Boolean)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[Option[Int]] = {
    (if(public) {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id < ${memo.id} AND is_private=0 ORDER BY ID LIMIT 1"
    } else {
      sql"SELECT id FROM memos WHERE user = ${memo.user} AND id < ${memo.id} ORDER BY ID LIMIT 1"
    }).map { rs => rs.int(1) }.single.future
  }

}
