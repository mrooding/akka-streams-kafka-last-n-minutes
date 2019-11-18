package me.mrooding.blog.kafka

import java.nio.charset.StandardCharsets
import java.time.Instant

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.pattern._
import akka.stream.Materializer
import akka.stream.scaladsl.{MergeLatest, Sink, Source}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser.decode
import me.mrooding.blog.AppConfig
import me.mrooding.blog.data.Price
import me.mrooding.blog.publish.PriceUpdater.UpdatePrice
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.{PartitionInfo, TopicPartition}

import scala.collection.SortedMap
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class KafkaPriceConsumerService(config: AppConfig, partitions: Seq[PartitionInfo], priceUpdater: ActorRef)(
  implicit
  val system: ActorSystem,
  implicit val materializer: Materializer
) extends LazyLogging {
  import KafkaPriceConsumerService._

  require(partitions.nonEmpty, "Cannot consume without any partitions")

  def start(timeHorizon: Instant): Future[Done] = {
    val consumerSettings = config.consumerSettings
    val kafkaSources: SortedMap[Partition, Source[ConsumerRecord[String, Array[Byte]], Control]] =
      SortedMap(partitions.map { partition =>
        val topicPartition = new TopicPartition(partition.topic(), partition.partition())
        val partitionSubscription = Subscriptions.assignmentOffsetsForTimes(topicPartition, timeHorizon.toEpochMilli)
        Partition(partition.partition()) -> Consumer.plainSource[String, Array[Byte]](consumerSettings, partitionSubscription)
      }: _*)

    implicit val timeout: Timeout = Timeout(10 seconds)
    val publisher = jsonPublisher(rfq => priceUpdater ? UpdatePrice(rfq), parallelism = 1)(system.dispatcher)
    val rfqPublishers: Seq[Source[(Partition, RecordPosition), Control]] = kafkaSources.map {
      case (partition, source) =>
        source.via(publisher).map(position => partition -> position)
    }.toSeq

    val compositeStream: Source[List[(Partition, RecordPosition)], _] = rfqPublishers match {
      case Seq(onlyPartition)            => onlyPartition.map(x => List(x))
      case Seq(first, second)            => Source.combine(first, second)(MergeLatest(_, eagerComplete = true))
      case Seq(first, second, rest @ _*) => Source.combine(first, second, rest: _*)(MergeLatest(_, eagerComplete = true))
    }
    compositeStream
      .log(getClass.getSimpleName)
      .runWith(Sink.ignore)
  }
}

object KafkaPriceConsumerService extends LazyLogging {
  import akka.stream.scaladsl._

  final case class Partition(id: Long) extends AnyVal

  object Partition {
    implicit def orderingById: Ordering[Partition] = Ordering.by(p => p.id)
  }
  final case class RecordPosition(offset: Long, timestamp: Instant)

  def jsonPublisher(publish: Price => Future[Any], parallelism: Int)(
    implicit executionContext: ExecutionContext
  ): Flow[ConsumerRecord[_, Array[Byte]], RecordPosition, NotUsed] =
    Flow[ConsumerRecord[_, Array[Byte]]]
      .map { record =>
        import me.mrooding.blog.json.PriceProtocol.priceFromKafkaJson

        val message = new String(record.value(), StandardCharsets.UTF_8)
        val decodingResult = decode(message)

        decodingResult.left
          .foreach(decodingFailure => logger.warn(s"Failed to parse ConsumerRecord to Price: $message", decodingFailure))

        RecordPosition(record.offset(), Instant.ofEpochMilli(record.timestamp())) -> decodingResult
      }
      .mapAsync(parallelism) {
        case (position, Right(rfq)) =>
          publish(rfq).map(_ => position)
        case (position, _) =>
          /* We logged the decoding failure above. We'll just skip the unprocessable RFQ for now */
          Future.successful(position)
      }
}
