package ru.yandex.practicum.collector.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);

            var writer = new org.apache.avro.specific.SpecificDatumWriter<SpecificRecordBase>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Ошибка сериализации Avro-объекта в топик " + topic, e);
        }
    }
}