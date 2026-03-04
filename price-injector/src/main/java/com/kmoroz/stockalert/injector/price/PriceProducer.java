package com.kmoroz.stockalert.injector.price;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;

@KafkaClient(id = "price-producer")
public interface PriceProducer {

      @Topic("market-prices")
      void sendPrice(@KafkaKey String symbol, PriceUpdateDto priceUpdate);
}
