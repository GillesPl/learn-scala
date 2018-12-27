package controllers

import akka.actor.ActorSystem
import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

@Api("Messages")
@Singleton
class MessageController @Inject()(system: ActorSystem,cc: ControllerComponents) extends AbstractController(cc) {


  @ApiOperation(nickname = "Get system health", value = "Get system health", notes = "Retuns a map with all the system health checks")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "System health check success"),
    new ApiResponse(code = 412, message = "Could not fetch the system health check")
  ))
  def getMessages = Action.async {

  }


  //get messages

  //get message

  //addMessage
}
