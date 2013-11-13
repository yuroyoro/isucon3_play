package models

import scalikejdbc.specs2.mutable.AutoRollback
import org.specs2.mutable._
import org.joda.time._
import scalikejdbc.SQLInterpolation._

class MemosSpec extends Specification {
  val m = Memos.syntax("m")

  "Memos" should {
    "find by primary keys" in new AutoRollback {
      val maybeFound = Memos.find(123)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollback {
      val allResults = Memos.findAll()
      allResults.size should be_>(0)
    }
    "count all records" in new AutoRollback {
      val count = Memos.countAll()
      count should be_>(0L)
    }
    "find by where clauses" in new AutoRollback {
      val results = Memos.findAllBy(sqls.eq(m.id, 123))
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Memos.countBy(sqls.eq(m.id, 123))
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Memos.create(user = 123, isPrivate = 1, createdAt = DateTime.now, updatedAt = DateTime.now)
      created should not beNull
    }
    "save a record" in new AutoRollback {
      val entity = Memos.findAll().head
      val updated = Memos.save(entity)
      updated should not equalTo(entity)
    }
    "destroy a record" in new AutoRollback {
      val entity = Memos.findAll().head
      Memos.destroy(entity)
      val shouldBeNone = Memos.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
        