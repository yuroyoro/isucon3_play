package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._

object Auth extends Controller with Helper {
  def signout = Action { implicit request =>
    requireUser{ user =>
      antiCsrf{ Results.Redirect("/").withNewSession }
    }
  }

  def index = Action { implicit request =>
    val user = getUser
    Ok(
      views.html.main(user, urlFor)(
        views.html.signin(user, urlFor)
      )
    )
  }

  def signin = Action { implicit request =>
    val form = request.body.asFormUrlEncoded
    (for {
      username <- form.get("username").headOption
      password <- form.get("password").headOption
      user     <- Users.findByName(username)
      if user.password  == sha256(user.salt + password)
    } yield {
      // session.clear()
      user.updateLastAccess
      Results.Redirect("/").withSession(
        "user_id" -> user.id.toString,
        "token"   -> sha256(scala.util.Random.nextInt.toString)
      )
    }).getOrElse {
      Ok(
        views.html.main(None, urlFor)(
          views.html.signin(None, urlFor)
        )
      )
    }
  }
}
