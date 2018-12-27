package controllers

import com.google.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class SwaggerController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  /**
    * Needed for swagger ui, in order to set the url to json spec dynamically.
    * Avoids to change it in the index.html file
    *
    * @return
    */
  def redirectDocs = Action {
    Redirect(url = "/assets/swagger-ui/index.html", queryString = Map("url" -> Seq("/swagger.json")))
  }

}
