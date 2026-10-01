package ru.yandex.practicum.analyzer.config;

import deserializer.HubEventDeserializer;
import deserializer.SensorsSnapshotDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;


import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {

    @Bean(destroyMethod = "")
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer(AnalyzerKafkaProperties props) {
        return new KafkaConsumer<>(
                consumerProperties(props.getBootstrapServers(), props.getHubConsumer(), HubEventDeserializer.class));
    }

    @Bean(destroyMethod = "")
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer(AnalyzerKafkaProperties props) {
        return new KafkaConsumer<>(
                consumerProperties(props.getBootstrapServers(), props.getSnapshotConsumer(), SensorsSnapshotDeserializer.class));
    }

    private Properties consumerProperties(String bootstrapServers,
                                          AnalyzerKafkaProperties.ConsumerSettings settings,
                                          Class<?> valueDeserializer) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, settings.getGroupId());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, settings.isEnableAutoCommit());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, settings.getAutoOffsetReset());
        return p;
    }
}