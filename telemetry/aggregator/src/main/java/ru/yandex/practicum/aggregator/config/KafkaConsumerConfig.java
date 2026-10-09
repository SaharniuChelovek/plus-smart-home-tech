package ru.yandex.practicum.aggregator.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;


import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public KafkaConsumer<String, SensorEventAvro> kafkaConsumer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.consumer.group-id}") String groupId,
            @Value("${kafka.consumer.key-deserializer}") String keyDeserializer,
            @Value("${kafka.consumer.value-deserializer}") String valueDeserializer,
            @Value("${kafka.consumer.enable-auto-commit}") boolean enableAutoCommit) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);

        return new KafkaConsumer<>(props);
    }
}