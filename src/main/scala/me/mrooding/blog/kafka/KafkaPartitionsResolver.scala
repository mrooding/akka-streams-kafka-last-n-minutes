package me.mrooding.blog.kafka

import akka.actor.ActorRef
import akka.kafka.Metadata
import akka.kafka.Metadata.GetPartitionsFor
import akka.pattern._
import akka.util.Timeout
import org.apache.kafka.common.PartitionInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class KafkaPartitionsResolver(consumer: ActorRef) {
  implicit val timeout: Timeout = Timeout(5 seconds)

  def retrievePartitionsForTopic(topic: String): Future[List[PartitionInfo]] = {
    val partitionsFor = (consumer ? GetPartitionsFor(topic)).mapTo[Metadata.PartitionsFor]

    partitionsFor.flatMap(p => Future.fromTry(p.response))
  }
}
