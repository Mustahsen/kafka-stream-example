package com.mergen.configuration;

import com.mergen.dto.EventDataDto;
import com.mergen.dto.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.test-scheduling", name = "enabled")
public class SchedulingConfiguration {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Random random = new Random();

    private final List<String> channels = Arrays.asList(
            "WEB",
            "IOS",
            "ANDROID"
    );

    private int p1Count;
    private int p2Count;
    private int clickCount;

    @Scheduled(fixedDelay = 1000L)
    public void produceImpressions() {
        Instant now = Instant.now(Clock.systemUTC());
        Date timestamp = Date.from(now);
        Date displayDate = Date.from(now.atZone(ZoneId.of("Turkey")).toLocalDate().atStartOfDay().atZone(ZoneId.of("UTC")).toInstant());

        boolean coinFlip = random.nextInt(100) % 2 == 0;

        EventDto eventDto = EventDto.builder()
                .channel(channels.get(random.nextInt(channels.size())))
                .timestamp(timestamp)
                .displayDate(displayDate)
                .eventId(UUID.randomUUID())
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("channel", channels.get(random.nextInt(channels.size())));
        if (coinFlip) {
            eventDto.setData(
                    Arrays.asList(
                            EventDataDto.builder()
                                    .productId(2000L)
                                    .sellerId(250L)
                                    .build(),
                            EventDataDto.builder()
                                    .productId(2001L)
                                    .sellerId(251L)
                                    .build()
                    )
            );
            p2Count++;
        } else {
            eventDto.setData(
                    Collections.singletonList(
                            EventDataDto.builder()
                                    .productId(2000L)
                                    .sellerId(250L)
                                    .build()
                    )
            );
        }
        p1Count++;
        kafkaTemplate.send("impression-topic", eventDto);
        log.info("Product: 2000 Impression Count: {}", p1Count);
        log.info("Product: 2001 Impression Count: {}", p2Count);
    }

    @Scheduled(fixedDelay = 10_000L)
    public void produceClick() {
        Instant now = Instant.now(Clock.systemUTC());
        Date timestamp = Date.from(now);
        Date displayDate = Date.from(now.atZone(ZoneId.of("Turkey")).toLocalDate().atStartOfDay().atZone(ZoneId.of("UTC")).toInstant());

        EventDto eventDto = EventDto.builder()
                .channel(channels.get(random.nextInt(channels.size())))
                .timestamp(timestamp)
                .displayDate(displayDate)
                .eventId(UUID.randomUUID())
                .data(
                        Collections.singletonList(EventDataDto.builder()
                                .productId(2000L)
                                .sellerId(250L)
                                .build())
                )
                .build();
        clickCount++;
        kafkaTemplate.send("click-topic", eventDto);
        log.info("Product: 2000 Click Count: {}", clickCount);
    }

}
