package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def slides = Action(implicit r => Ok(views.html.slides(true)))

  def searchWords = Action(Ok(views.html.searchWords()))

}
