package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def slides(withExamples: Boolean) = Action(implicit r => Ok(views.html.slides(withExamples)))

  def searchWords = Action(Ok(views.html.searchWords()))

}
