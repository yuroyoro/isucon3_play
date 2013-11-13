package controllers

import play.api._
import play.api.mvc._
import play.api.libs.Files._
import models._

import scala.concurrent._
import scalikejdbc.SQLInterpolation._

object Application extends Controller with Helper{

  def index = Action { implicit request =>

    val user = getUser
    val total = Memos.total
    val memos = Memos.public.map{
      m => m.copy( username = Users.nameOf(m.user))
    }
    val urlFor = (path:String) => {
      s"http://${request.host}${path}"
    }

    Ok(
      views.html.main(user, urlFor)(
        views.html.index(total, memos, 0, urlFor)
      )
    )
  }
}

case class RequireUser[A](action: Users => Action[A]) extends Helper{

  def apply(request: Request[A]): Future[SimpleResult] = {
    getUser(request).map{ user =>
      action(user)(request)
    }.getOrElse {
      Future.successful(Results.Redirect("/"))
    }
  }
}

case class AntiCsrf[A](action: Action[A]) extends Action[A] with Helper {

  def apply(request: Request[A]): Future[SimpleResult] = {
    (for{
      sid   <- request.queryString.get("sid")
      token <- request.session.get("token")
      if sid == token
    } yield {
      action(request)
    }).getOrElse {
      Future.successful(Results.BadRequest)
    }
  }

  lazy val parser = action.parser
}

trait Helper {

  import sys.process._
  import java.io._

  def getUser[A](implicit request:Request[A]) :Option[Users] =
    request.session.get("user_id").flatMap{user_id => Users.find(user_id.toInt) }

  def genMarkdown(md:String):String = {
    val tmp = File.createTempFile("isucontemp", "")
    val out = new BufferedWriter(new FileWriter(tmp))
    out.write(md)
    out.close

    val html = s"/home/isucon/webapp/bin/markdown ${tmp.getAbsolutePath}"!!

    tmp.delete
    html
  }
}
