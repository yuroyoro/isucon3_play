package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._

object Application extends Controller with Helper{

  def index = Action.async { implicit request =>
    val user = getUser
    val total = Memos.total
    val memos = Memos.public(0)
    Ok(
      views.html.main(user, urlFor)(
        views.html.index(total, memos, 0, urlFor)
      )
    )
  }

  def recent(page:Int) = Action.async { implicit request =>
    val total = Memos.total
    val user = getUser

    Memos.public(page) match {
      case Nil   => Results.NotFound
      case xs =>
        val memos = xs
        Ok(
          views.html.main(user, urlFor)(
            views.html.index(total, memos, page, urlFor)
          )
        )
    }
  }

}

