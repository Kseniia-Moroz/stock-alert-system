package com.kmoroz.stockalert.alert.engine;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient(id = "notification-producer")
public interface NotificationProducer {

    @Topic("user-notifications")
    void sendNotification(@KafkaKey String id, String notification);
}
