package me.mrooding.blog.data

import java.time.Instant

case class Price(isin: String, price: BigDecimal, timestamp: Instant)
