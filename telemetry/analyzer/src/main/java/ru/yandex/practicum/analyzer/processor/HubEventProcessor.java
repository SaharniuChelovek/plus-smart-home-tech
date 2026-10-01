package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;
    private final AnalyzerKafkaProperties properties;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(properties.getTopics().getHubs()));
            Duration pollTimeout = Duration.ofMillis(properties.getHubConsumer().getPollTimeoutMs());

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(pollTimeout);

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        hubEventService.handle(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка обработки события хаба (partition={}, offset={})",
                                record.partition(), record.offset(), e);
                    }
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