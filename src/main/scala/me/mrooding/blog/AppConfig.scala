package me.mrooding.blog

import akka.kafka.ConsumerSettings
import com.typesafe.config.{Config, ConfigFactory}
import me.mrooding.blog.AppConfig.{KafkaConfig, KafkaTopics}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

class AppConfig(
                  config: Config = ConfigFactory.load().getConfig("me.mrooding.blog"),
                  akkaKafkaConfig: Config = ConfigFactory.load().getConfig("akka.kafka")
                ) {
  val kafka = KafkaConfig(
    groupId = config.getString("kafka.group-id"),
    brokerList = config.getString("kafka.broker-list"),
    topics = KafkaTopics(
      price = config.getString("kafka.topics.price"),
    ),
    bufferSize = config.getInt("kafka.producer.buffer-size")
  )

  val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(akkaKafkaConfig.getConfig("consumer"), new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(kafka.brokerList)
      .withGroupId(kafka.groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
}

object AppConfig {
  case class KafkaTopics(price: String)
  case class KafkaConfig(groupId: String, brokerList: String, topics: KafkaTopics, bufferSize: Int)
}

