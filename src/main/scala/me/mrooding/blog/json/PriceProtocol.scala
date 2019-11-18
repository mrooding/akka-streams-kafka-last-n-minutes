package me.mrooding.blog.json

import io.circe.Decoder
import io.circe.generic.semiauto
import me.mrooding.blog.data.Price

trait PriceProtocol {
  implicit val priceFromKafkaJson: Decoder[Price] = semiauto.deriveDecoder
}

object PriceProtocol extends PriceProtocol
