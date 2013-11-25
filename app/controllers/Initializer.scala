package controllers

import play.api._
import play.api.mvc._
import models._

import scala.concurrent._

object Initializer extends Controller with Helper{

  def index = Action { implicit request =>
    UsersCache.load()
    Ok("initialized")
  }

}


