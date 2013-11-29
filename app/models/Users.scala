package models

import scalikejdbc._, async._, FutureImplicits._, SQLInterpolation._
import scala.concurrent._
import org.joda.time.{DateTime}
import scala.concurrent.ExecutionContext.Implicits.global

case class Users(
  id: Int,
  username: String,
  password: String,
  salt: String,
  lastAccess: Option[DateTime] = None) extends ShortenedNames{

  def save()(implicit session: AsyncDBSession = AsyncDB.sharedSession,  cxt: EC = ECGlobal): Future[Users] = Users.save(this)(session, cxt)

  def destroy()(implicit session: AsyncDBSession = AsyncDB.sharedSession,  cxt: EC = ECGlobal): Future[Int] = Users.destroy(this)(session, cxt)

  def updateLastAccess()(implicit session: AsyncDBSession = AsyncDB.sharedSession,  cxt: EC = ECGlobal): Future[Int] = Users.updateLastAccess(this)(session, cxt)
}


object Users extends SQLSyntaxSupport[Users] with ShortenedNames{

  override val tableName = "users"

  override val columns = Seq("id", "username", "password", "salt", "last_access")

  def apply(u: ResultName[Users])(rs: WrappedResultSet): Users = new Users(
    id = rs.int(u.id),
    username = rs.string(u.username),
    password = rs.string(u.password),
    salt = rs.string(u.salt),
    lastAccess = rs.timestampOpt(u.lastAccess).map(_.toDateTime)
  )

  val u = Users.syntax("u")

  val autoSession = AutoSession

  def nameOf(id: Int)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Option[String]] = {
    withSQL {
      select(u.username).from(Users as u).where.eq(u.id, id)
    }.map(rs => rs.string(1)).single.future
  }

  def findByName(username:String)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal):Future[Option[Users]] = {
    withSQL {
      select.from(Users as u).where.eq(u.username, username)
    }.map(Users(u.resultName)).single.future
  }

  def find(id: Int)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Option[Users]] = {
    withSQL {
      select.from(Users as u).where.eq(u.id, id)
    }.map(Users(u.resultName)).single.future
  }

  def findAll()(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[List[Users]] = {
    withSQL(select.from(Users as u)).map(Users(u.resultName)).list.future
  }

  def countAll()(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    withSQL(select(sqls"count(1)").from(Users as u)).map(rs => rs.long(1)).single.future.map(_.get)
  }

  def findAllBy(where: SQLSyntax)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[List[Users]] = {
    withSQL {
      select.from(Users as u).where.append(sqls"${where}")
    }.map(Users(u.resultName)).list.future
  }

  def countBy(where: SQLSyntax)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Long] = {
    withSQL {
      select(sqls"count(1)").from(Users as u).where.append(sqls"${where}")
    }.map(_.long(1)).single.future.map(_.get)
  }

  def create(
    username: String,
    password: String,
    salt: String,
    lastAccess: Option[DateTime] = None)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Users] = {
    for{
      id <- withSQL {
        insert.into(Users).columns(
          column.username,
          column.password,
          column.salt,
          column.lastAccess
        ).values(
          username,
          password,
          salt,
          lastAccess
        )
      }.updateAndReturnGeneratedKey()
    } yield {
      Users(
        id = id.toInt,
        username = username,
        password = password,
        salt = salt,
        lastAccess = lastAccess)
    }
  }

  def save(entity: Users)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Users] = {
    withSQL {
      update(Users as u).set(
        u.id -> entity.id,
        u.username -> entity.username,
        u.password -> entity.password,
        u.salt -> entity.salt,
        u.lastAccess -> entity.lastAccess
      ).where.eq(u.id, entity.id)
    }.update.future.map{_ => entity }
  }

  def destroy(entity: Users)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Int] = {
    withSQL { delete.from(Users).where.eq(column.id, entity.id) }.update.future
  }

  def updateLastAccess(entity: Users)(implicit session: AsyncDBSession = AsyncDB.sharedSession, cxt: EC = ECGlobal): Future[Int] = {
    withSQL {
      update(Users as u).set(sqls"last_access=now()").where.eq(u.id, entity.id)
    }.update.future
  }
}

object UsersCache {
  var __cache:Map[Int, Users] = Map.empty[Int, Users]

  def load() :Unit = {
    Users.findAll.map{ ul => ul.map{ u => (u.id,  u) }.toMap }.foreach{map =>
      __cache = map
    }
  }

  def apply(user_id:Int):Users = __cache(user_id)
  def get(user_id:Int):Option[Users] = __cache.get(user_id)

  def nameOf(user_id:Int):Option[String] = __cache.get(user_id).map{_.username}

  def findByName(username:String):Option[Users] = __cache.values.find(_.username == username)
}
