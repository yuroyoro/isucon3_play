package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._

trait Helper {

  import sys.process._
  import java.io._
  import play.api.http.HeaderNames

  implicit def futurize[A](result:SimpleResult):Future[SimpleResult] = Future.successful(result)

  def requireUser[A](f:Users => SimpleResult)(implicit request:Request[A]):SimpleResult = {
    getUser(request).map{ user =>
      appendCacheControl(
        f(user)
      )
    }.getOrElse {
      Results.Redirect("/")
    }
  }

  def antiCsrf[A](formOpts:Option[Map[String, Seq[String]]])(f: => SimpleResult)(implicit request:Request[A]):SimpleResult = {
    (for{
      form  <- formOpts
      sid   <- form.get("sid").flatMap(_.headOption)
      token <- request.session.get("token")
      if sid == token
    } yield {
      f
    }).getOrElse {
      Results.BadRequest
    }
  }

  def getUser[A](implicit request:Request[A]) :Option[Users] = {
    request.session.get("user_id").flatMap{user_id => UsersCache.get(user_id.toInt) }
  }

  def appendCacheControl(result:SimpleResult):SimpleResult =
    result.withHeaders( HeaderNames.CACHE_CONTROL -> "private")

  // val markdownCmd = "/Users/ozaki/dev/isucon/webapp/bin/markdown"
  val markdownCmd = "/home/isucon/webapp/bin/markdown"

  import eu.henkelmann.actuarius.ActuariusTransformer

  val transformer = new ActuariusTransformer()

  def genMarkdown(md:String):String = transformer(md)

  def urlFor[A](implicit request:Request[A]):String => String = (path:String) => s"http://${request.host}${path}"

  def sha256(s:String) = {
    import java.security.MessageDigest
    val digestedBytes = MessageDigest.getInstance("SHA-256").digest(s.getBytes)
    digestedBytes.map("%02x".format(_)).mkString
  }
}

