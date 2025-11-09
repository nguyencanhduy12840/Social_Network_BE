package com.socialapp.notificationservice.config;

import com.socialapp.notificationservice.dto.FriendshipEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FriendshipEventDTO> friendshipEventKafkaListenerContainerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // JsonDeserializer cấu hình chuẩn cho DTO
        JsonDeserializer<FriendshipEventDTO> deserializer = new JsonDeserializer<>(FriendshipEventDTO.class);
        deserializer.addTrustedPackages("*");
        deserializer.setRemoveTypeHeaders(false);
        deserializer.setUseTypeMapperForKey(false);
        deserializer.ignoreTypeHeaders(); // ignore header nếu producer không gửi __TypeId__

        DefaultKafkaConsumerFactory<String, FriendshipEventDTO> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);

        ConcurrentKafkaListenerContainerFactory<String, FriendshipEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Error handler để log và skip lỗi
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, exception) -> {
                    System.err.println("Skipping bad record: " + record.value());
                    exception.printStackTrace();
                }
        ));

        return factory;
    }

}
