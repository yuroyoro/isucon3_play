package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._
import org.joda.time.{DateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scalikejdbc._, SQLInterpolation._, async._

object Memo extends Controller with Helper{

  def mypage = Action.async { implicit request =>
    requireUser { user =>
      Memos.findByUser(user.id).map{ memos =>
        Ok( views.html.main(Some(user), urlFor)( views.html.mypage(user, memos, urlFor)))
      }
    }
  }

  def show(memoId:Int) = Action.async { implicit request =>
    val user = getUser

    (for {
      mmemo   <- Memos.find(memoId).map{ mo => mo.filter{ m => m.isPrivate != 1 || !user.filter{ _.id == m.user }.isEmpty }}
      mpublic =  mmemo.map{ memo =>  user.filter{ _.id == memo.user}.isEmpty }
      mnewer  <- mmemo.flatMap{ memo => mpublic.map{ public => Memos.newer(memo, public) }}.getOrElse { Future.successful(None) }
      molder  <- mmemo.flatMap{ memo => mpublic.map{ public => Memos.older(memo, public) }}.getOrElse { Future.successful(None) }
    } yield {
      mmemo.map{ memo =>
        val markdownized = memo.copy(contentHtml = memo.content.map{genMarkdown(_)})
        Ok( views.html.main(user, urlFor)( views.html.memo(user, markdownized, molder, mnewer, urlFor)))
      }.getOrElse(Results.NotFound)
    })
  }

  def create = Action.async { implicit request =>
    requireUser { user =>
      val formOpts = request.body.asFormUrlEncoded
      antiCsrf(formOpts) {
        val now = new DateTime()
        val content   = formOpts.flatMap(_.get("content")).flatMap(_.headOption)
        AsyncDB.localTx { implicit tx =>
          for{
            memo <- (Memos.create(
                      user = user.id,
                      content   = content,
                      title     = content.flatMap(_.split("""\r?\n""").headOption),
                      isPrivate = formOpts.flatMap(_.get("is_private")).flatMap(_.headOption).map{_.toByte}.getOrElse(0x0),
                      createdAt = now,
                      updatedAt = now
                    ))
            _ <- Memos.insertPublicMemos(memo)
            _ <- Memos.incrementCount(memo)
          } yield {
            Results.Redirect(s"/memo/${memo.id}")
          }
        }
      }
    }
  }
}

