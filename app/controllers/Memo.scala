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
    (for{
      m <- Memos.find(memoId)
      if m.isPrivate != 1 || !user.filter{ _.id == m.user }.isEmpty
    } yield {
      val memo = m.copy(contentHtml = m.content.map{genMarkdown(_)})
      val public = user.filter{ _.id == memo.user}.isEmpty
      val newer = Memos.newer(memo, public)
      val older = Memos.older(memo, public)
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
      val formOpts = request.body.asFormUrlEncoded
      antiCsrf(formOpts) {
        val now = new DateTime()
        val content   = formOpts.flatMap(_.get("content")).flatMap(_.headOption)
        val memo = Memos.create(
          user = user.id,
          content   = content,
          title     = content.flatMap(_.split("""\r?\n""").headOption),
          isPrivate = formOpts.flatMap(_.get("is_private")).flatMap(_.headOption).map{_.toByte}.getOrElse(0x0),
          createdAt = now,
          updatedAt = now
        )
        if(memo.isPrivate == 0) {
          Memos.insertPublicMemos(memo.id)
          Memos.incrementCount
        }
        Results.Redirect(s"/memo/${memo.id}")
      }
    }
  }
}

