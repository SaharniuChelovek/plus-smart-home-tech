package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "analyzer.kafka")
public class AnalyzerKafkaProperties {

    private String bootstrapServers;
    private Topics topics = new Topics();
    private ConsumerSettings hubConsumer = new ConsumerSettings();
    private ConsumerSettings snapshotConsumer = new ConsumerSettings();

    @Getter
    @Setter
    public static class Topics {
        private String hubs;
        private String snapshots;
    }

    @Getter
    @Setter
    public static class ConsumerSettings {
        private String groupId;
        private boolean enableAutoCommit;
        private String autoOffsetReset = "latest";
        private long pollTimeoutMs = 1000;
    }
}