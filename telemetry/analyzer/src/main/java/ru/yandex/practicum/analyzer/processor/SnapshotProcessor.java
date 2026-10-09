package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.client.HubRouterClient;
import ru.yandex.practicum.analyzer.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.service.ScenarioAnalysisService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final ScenarioAnalysisService analysisService;
    private final HubRouterClient hubRouterClient;
    private final AnalyzerKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(snapshotConsumer::wakeup));

        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        try {
            snapshotConsumer.subscribe(List.of(properties.getTopics().getSnapshots()));
            Duration pollTimeout = Duration.ofMillis(properties.getSnapshotConsumer().getPollTimeoutMs());

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = snapshotConsumer.poll(pollTimeout);

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        analysisService.analyze(record.value()).forEach(hubRouterClient::send);
                    } catch (Exception e) {
                        log.error("Ошибка обработки снапшота (partition={}, offset={})",
                                record.partition(), record.offset(), e);
                    }

                    TopicPartition partition = new TopicPartition(record.topic(), record.partition());
                    OffsetAndMetadata offset = new OffsetAndMetadata(record.offset() + 1);
                    currentOffsets.put(partition, offset);

                    snapshotConsumer.commitAsync(Map.of(partition, offset), (offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Не удалось зафиксировать смещения {}", offsets, exception);
                        }
                    });
                }
            }
        } catch (WakeupException ignored) {
            // штатная остановка
        } catch (Exception e) {
            log.error("Ошибка в цикле обработки снапшотов", e);
        } finally {
            try {
                snapshotConsumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер снапшотов");
                snapshotConsumer.close();
            }
        }
    }
}