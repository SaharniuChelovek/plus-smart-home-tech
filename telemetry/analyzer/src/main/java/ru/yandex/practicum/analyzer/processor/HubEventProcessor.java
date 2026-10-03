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
import ru.yandex.practicum.analyzer.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;
    private final AnalyzerKafkaProperties properties;

    private static final int MAX_ATTEMPTS = 3;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        Map<TopicPartition, Integer> failedAttempts = new HashMap<>();

        try {
            consumer.subscribe(List.of(properties.getTopics().getHubs()));
            Duration pollTimeout = Duration.ofMillis(properties.getHubConsumer().getPollTimeoutMs());

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(pollTimeout);
                Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

                for (TopicPartition tp : records.partitions()) {
                    for (ConsumerRecord<String, HubEventAvro> record : records.records(tp)) {
                        try {
                            hubEventService.handle(record.value());
                            failedAttempts.remove(tp);
                            offsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                        } catch (Exception e) {
                            int attempt = failedAttempts.merge(tp, 1, Integer::sum);
                            if (attempt < MAX_ATTEMPTS) {
                                log.warn("Ошибка обработки (partition={}, offset={}), попытка {}/{}",
                                        tp.partition(), record.offset(), attempt, MAX_ATTEMPTS, e);
                                consumer.seek(tp, record.offset()); // вернёмся к этой записи
                                break; // остальные записи партиции не трогаем, чтобы не нарушить порядок
                            }
                            log.error("Событие пропущено после {} попыток (partition={}, offset={}): {}",
                                    MAX_ATTEMPTS, tp.partition(), record.offset(), record.value(), e);
                            // здесь же можно отправить в dead letter topic
                            failedAttempts.remove(tp);
                            offsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                        }
                    }
                }

                if (!offsets.isEmpty()) {
                    consumer.commitSync(offsets);
                }
            }
        } catch (WakeupException ignored) {
            // штатная остановка
        } catch (Exception e) {
            log.error("Ошибка в цикле обработки событий хабов", e);
        } finally {
            log.info("Закрываем консьюмер событий хабов");
            consumer.close();
        }
    }
}