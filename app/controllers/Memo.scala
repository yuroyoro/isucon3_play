package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._
import org.joda.time.{DateTime}

object Memo extends Controller with Helper{

  def mypage = Action { implicit request =>
    requireUser { user =>
      val memos = Memos.findByUser(user.id)
      Ok(
        views.html.main(Some(user), urlFor)(
          views.html.mypage(user, memos, urlFor)
        )
      )
    }
  }

  def show(memoId:Int) = Action { implicit request =>
    val user = getUser
    Console.println(Memos.find((memoId)))
    (for{
      m <- Memos.find(memoId)
      if m.isPrivate != 1 || !user.filter{ _.id == m.user }.isEmpty
    } yield {
      val memo = m.copy(username = Users.nameOf(m.user), contentHtml = m.content.map{genMarkdown(_)})
      val memos = user.filter{ _.id == memo.user}.map{ user =>
        Memos.findByUser(memo.user)
      }.getOrElse {
        Memos.findPublicByUser(memo.user)
      }

      val (newerSeq, olderSeq) = memos.span{ _.id != memo.id }
      val newer = newerSeq.headOption
      val older = olderSeq.tail.headOption
      Ok(
        views.html.main(user, urlFor)(
          views.html.memo(user, memo, older, newer, urlFor)
        )
      )
    }).getOrElse {
      Results.NotFound
    }
  }

  def create = Action { implicit request =>
    requireUser { user =>
      val form = request.body.asFormUrlEncoded
      val now = new DateTime()
      val memo = Memos.create(
        user = user.id,
        content = form.get("content").headOption,
        isPrivate= form.get("is_private").headOption.map{_.toByte}.getOrElse(0x0),
        createdAt = now,
        updatedAt = now
      )
      Results.Redirect(s"/memo/${memo.id}")
    }
  }
}

