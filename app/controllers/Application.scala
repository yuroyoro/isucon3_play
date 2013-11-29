package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with Helper{

  def index = Action.async { implicit request =>

    val user = getUser
    for{
      total <- Memos.total
      memos <- Memos.public(total, 0)
    } yield {
      Ok(views.html.main(user, urlFor)(views.html.index(total, memos, 0, urlFor)))
    }
  }

  def recent(page:Int) = Action.async { implicit request =>
    val user = getUser

    for {
      total <- Memos.total
      memos <- Memos.public(total, page)
    } yield {
      memos match {
        case Nil => Results.NotFound
        case xs  => Ok(views.html.main(user, urlFor)(views.html.index(total, xs, page, urlFor)))
      }
    }
  }

}

