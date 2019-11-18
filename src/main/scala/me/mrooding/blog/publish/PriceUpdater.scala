package me.mrooding.blog.publish

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import me.mrooding.blog.data.Price
import me.mrooding.blog.publish.PriceUpdater.UpdatePrice

import scala.concurrent.ExecutionContext

class PriceUpdater()(implicit system: ActorSystem, execContext: ExecutionContext)
  extends Actor
    with ActorLogging {
  override def receive: Receive = {
    case UpdatePrice(price) =>
      log.debug(s"Updating price: $price")
      sender() ! Done
  }
}

object PriceUpdater {

  case class UpdatePrice(price: Price)

  def props()(implicit system: ActorSystem, context: ExecutionContext) =
    Props(new PriceUpdater())
}
