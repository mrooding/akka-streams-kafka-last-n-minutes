package me.mrooding.blog

import java.time.{Duration, Instant}

import akka.actor.ActorSystem
import akka.kafka.KafkaConsumerActor
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import me.mrooding.blog.kafka.{KafkaPartitionsResolver, KafkaPriceConsumerService}
import me.mrooding.blog.publish.PriceUpdater

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object Main extends App with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("mune")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  Try(new AppConfig()) match {
    case Success(config) =>
      val kafkaConsumer = system.actorOf(KafkaConsumerActor.props(config.consumerSettings))
      val kafkaPartitionsResolver = new KafkaPartitionsResolver(kafkaConsumer)

      kafkaPartitionsResolver
        .retrievePartitionsForTopic(config.kafka.topics.price)
        .flatMap { partitions =>
          val priceUpdater = system.actorOf(PriceUpdater.props())
          val kafkaRFQConsumerService = new KafkaPriceConsumerService(config, partitions, priceUpdater)

          val timeHorizon = Duration.ofMinutes(5)
          val horizonStart = Instant.now().minus(timeHorizon)

          val consumerServiceDone = kafkaRFQConsumerService.start(horizonStart)

          consumerServiceDone
            .andThen {
              case Success(_) =>
                logger.info("RFQ consumer service has stopped.")
              case Failure(exception) =>
                logger.error("RFQ consumer service has stopped due to error.", exception)
            }
        }
  }
}
