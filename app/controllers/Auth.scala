package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object Auth extends Controller with Helper {

  def signout = Action.async { implicit request =>
    requireUser{ user =>
      val formOpts = request.body.asFormUrlEncoded
      antiCsrf(formOpts){ Results.Redirect("/").withNewSession }
    }
  }

  def index = Action.async { implicit request =>
    val user = getUser
    Ok( views.html.main(user, urlFor)( views.html.signin(user, urlFor))
    )
  }

  def signin = Action.async { implicit request =>
    val formOpts = request.body.asFormUrlEncoded
    val result:Option[SimpleResult] = (for {
      form     <- formOpts
      username <- form("username").headOption
      password <- form("password").headOption
      user     <- UsersCache.findByName(username)
      if user.password  == sha256(user.salt + password)
    } yield {
      // session.clear()
      // user.updateLastAccess
      appendCacheControl(
        Results.Redirect("/mypage").withSession(
          "user_id" -> user.id.toString,
          "token"   -> sha256(scala.util.Random.nextInt.toString)
        )
      )
    })

    lazy val fallback:SimpleResult  = Ok( views.html.main(None, urlFor)( views.html.signin(None, urlFor)))
    val res:SimpleResult = result.getOrElse(fallback)
    futurize(res)
  }
}
