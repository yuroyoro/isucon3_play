package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._

trait Helper {

  import sys.process._
  import java.io._

  implicit def futurize[A](result:SimpleResult):Future[SimpleResult] = Future.successful(result)

  def requireUser[A](f:Users => SimpleResult)(implicit request:Request[A]):SimpleResult = {
    getUser(request).map{ user =>
      f(user)
    }.getOrElse {
      Results.Redirect("/")
    }
  }

  def antiCsrf[A](f: => SimpleResult)(implicit request:Request[A]):SimpleResult = {
    (for{
      sid   <- request.queryString.get("sid")
      token <- request.session.get("token")
      if sid == token
    } yield {
      f
    }).getOrElse {
      Results.BadRequest
    }
  }

  def getUser[A](implicit request:Request[A]) :Option[Users] = {
    scala.Console.println(s"user_id : ${request.session.get("user_id")}")
    request.session.get("user_id").flatMap{user_id => Users.find(user_id.toInt) }
  }

  val markdownCmd = "/Users/ozaki/dev/isucon/webapp/bin/markdown"
  def genMarkdown(md:String):String = {
    val tmp = File.createTempFile("isucontemp", "")
    val out = new BufferedWriter(new FileWriter(tmp))
    out.write(md)
    out.close

    val html = s"${markdownCmd} ${tmp.getAbsolutePath}"!!

    tmp.delete
    html
  }

  def urlFor[A](implicit request:Request[A]):String => String = (path:String) => s"http://${request.host}${path}"

  def sha256(s:String) = {
    import java.security.MessageDigest
    val digestedBytes = MessageDigest.getInstance("SHA-256").digest(s.getBytes)
    digestedBytes.map("%02x".format(_)).mkString
  }
}

