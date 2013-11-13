package models

import scalikejdbc.specs2.mutable.AutoRollback
import org.specs2.mutable._
import org.joda.time._
import scalikejdbc.SQLInterpolation._

class UsersSpec extends Specification {
  val u = Users.syntax("u")

  "Users" should {
    "find by primary keys" in new AutoRollback {
      val maybeFound = Users.find(123)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollback {
      val allResults = Users.findAll()
      allResults.size should be_>(0)
    }
    "count all records" in new AutoRollback {
      val count = Users.countAll()
      count should be_>(0L)
    }
    "find by where clauses" in new AutoRollback {
      val results = Users.findAllBy(sqls.eq(u.id, 123))
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Users.countBy(sqls.eq(u.id, 123))
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Users.create(username = "MyString", password = "MyString", salt = "MyString")
      created should not beNull
    }
    "save a record" in new AutoRollback {
      val entity = Users.findAll().head
      val updated = Users.save(entity)
      updated should not equalTo(entity)
    }
    "destroy a record" in new AutoRollback {
      val entity = Users.findAll().head
      Users.destroy(entity)
      val shouldBeNone = Users.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
        