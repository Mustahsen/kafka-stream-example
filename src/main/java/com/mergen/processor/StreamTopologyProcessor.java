package com.mergen.processor;

import com.mergen.dto.EventDataDto;
import com.mergen.dto.EventDto;
import com.mergen.dto.FlattenedEventDto;
import com.mergen.dto.ProductAggregationDto;
import com.mergen.dto.SellerAggregationDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;

@Slf4j
@Component
public class StreamTopologyProcessor {
    
    private final Serde<EventDto> eventDtoSerde;
    private final Serde<FlattenedEventDto> flattenedEventDtoSerde;
    private final Serde<ProductAggregationDto> productAggregationDtoSerde;
    private final Serde<SellerAggregationDto> sellerAggregationDtoSerde;

    @Getter
    private final Long windowDuration = 10000L;

    @Getter
    private final Long graceDuration = 10L;
    
    @Getter
    private KStream<String, EventDto> impressionEventDtoKStream;

    @Getter
    private KStream<String, EventDto> clickEventDtoKStream;

    @Getter
    private KStream<String, FlattenedEventDto> flattenedEventDtoKStream;

    @Getter
    private KStream<String, FlattenedEventDto> aggregationDtoKStream;

    @Getter
    private KStream<String, ProductAggregationDto> productAggregationDtoKStream;

    @Getter
    private KStream<String, SellerAggregationDto> sellerAggregationDtoKStream;

    public StreamTopologyProcessor() {
        this.eventDtoSerde = new JsonSerde<>(EventDto.class);
        this.flattenedEventDtoSerde = new JsonSerde<>(FlattenedEventDto.class);
        this.productAggregationDtoSerde = new JsonSerde<>(ProductAggregationDto.class);
        this.sellerAggregationDtoSerde = new JsonSerde<>(SellerAggregationDto.class);
    }

    @Autowired
    public void process(StreamsBuilder builder) {
        impressionEventDtoKStream = buildImpressionEventKStream(builder);
        clickEventDtoKStream = buildClickEventKStream(builder);
        streamImpressionToFlattenedEventTopic();
        streamClickToFlattenedEventTopic();
        flattenedEventDtoKStream = buildFlattenedDtoKStream(builder);
        aggregationDtoKStream = buildAggregationKStream(builder);
        productAggregationDtoKStream = buildProductAggregationKStream(builder);
        sellerAggregationDtoKStream = buildSellerAggregationCostKStream(builder);
        notifyExcessImpression();
    }

    private KStream<String, EventDto> buildImpressionEventKStream(StreamsBuilder builder) {
        return builder.stream("impression-topic", Consumed.with(Serdes.String(), eventDtoSerde));
    }

    private KStream<String, EventDto> buildClickEventKStream(StreamsBuilder builder) {
        return builder.stream("click-topic", Consumed.with(Serdes.String(), eventDtoSerde));
    }

    private void streamImpressionToFlattenedEventTopic() {
        impressionEventDtoKStream
                .flatMap((KeyValueMapper<String, EventDto, Iterable<KeyValue<String, FlattenedEventDto>>>)
                        (key, value) -> getImpressionFlattenedEvents(value))
                .to("flattened-event-topic", Produced.with(Serdes.String(), flattenedEventDtoSerde));
    }

    private List<KeyValue<String, FlattenedEventDto>> getImpressionFlattenedEvents(EventDto eventDto) {
        List<KeyValue<String, FlattenedEventDto>> aggregationDtos = new ArrayList<>();
        FlattenedEventDto flattenedEventDto = FlattenedEventDto.builder()
                .displayDate(eventDto.getDisplayDate())
                .channel(eventDto.getChannel())
                .build();
        List<EventDataDto> data = Optional.ofNullable(eventDto.getData()).orElse(Collections.emptyList());
        for (EventDataDto eventDataDto : data) {
            flattenedEventDto.setSellerId(eventDataDto.getSellerId());
            flattenedEventDto.setProductId(eventDataDto.getProductId());
            flattenedEventDto.setImpressionCount(1L);
            aggregationDtos.add(new KeyValue<>(flattenedEventDto.getAggregationId(), flattenedEventDto));
        }
        return aggregationDtos;
    }

    private void streamClickToFlattenedEventTopic() {
        clickEventDtoKStream
                .flatMap((KeyValueMapper<String, EventDto, Iterable<KeyValue<String, FlattenedEventDto>>>)
                        (key, value) -> getClickFlattenedEvents(value))
                .to("flattened-event-topic", Produced.with(Serdes.String(), flattenedEventDtoSerde));
    }

    private List<KeyValue<String, FlattenedEventDto>> getClickFlattenedEvents(EventDto eventDto) {
        List<KeyValue<String, FlattenedEventDto>> aggregationDtos = new ArrayList<>();
        FlattenedEventDto flattenedEventDto = FlattenedEventDto.builder()
                .displayDate(eventDto.getDisplayDate())
                .channel(eventDto.getChannel())
                .build();
        List<EventDataDto> data = Optional.ofNullable(eventDto.getData()).orElse(Collections.emptyList());
        if (!CollectionUtils.isEmpty(data)) {
            EventDataDto eventDataDto = data.get(0);
            flattenedEventDto.setSellerId(eventDataDto.getSellerId());
            flattenedEventDto.setProductId(eventDataDto.getProductId());
            flattenedEventDto.setClickCount(1L);
            aggregationDtos.add(new KeyValue<>(flattenedEventDto.getAggregationId(), flattenedEventDto));
        }
        return aggregationDtos;
    }

    private KStream<String, FlattenedEventDto> buildFlattenedDtoKStream(StreamsBuilder builder) {
        return builder.stream("flattened-event-topic", Consumed.with(Serdes.String(), flattenedEventDtoSerde));
    }

    private KStream<String, FlattenedEventDto> buildAggregationKStream(StreamsBuilder builder) {
        flattenedEventDtoKStream
                .groupByKey(Grouped.with(Serdes.String(), flattenedEventDtoSerde))
                .reduce(FlattenedEventDto::add, Materialized.as("flattened-event-table"))
                .toStream()
                .groupByKey(Grouped.with(Serdes.String(), flattenedEventDtoSerde))
                .windowedBy(TimeWindows
                        .of(Duration.of(windowDuration, MILLIS))
                        .grace(Duration.of(graceDuration, MILLIS))
                )
                .reduce((previous, next) -> next)
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                .to("aggregation-topic", Produced.with(Serdes.String(), flattenedEventDtoSerde));
        return builder.stream("aggregation-topic", Consumed.with(Serdes.String(), flattenedEventDtoSerde));
    }

    private KStream<String, ProductAggregationDto> buildProductAggregationKStream(StreamsBuilder builder) {
        aggregationDtoKStream
                .map((key, value) -> {
                    ProductAggregationDto productAggregationDto = ProductAggregationDto.builder()
                            .displayDate(value.getDisplayDate())
                            .sellerId(value.getSellerId())
                            .productId(value.getProductId())
                            .impressionCount(value.getImpressionCount())
                            .clickCount(value.getClickCount())
                            .build();
                    return new KeyValue<>(productAggregationDto.getAggregationId(), productAggregationDto);
                })
                .groupByKey(Grouped.with(Serdes.String(), productAggregationDtoSerde))
                .reduce(ProductAggregationDto::add, Materialized.as("product-aggregation-table"))
                .toStream()
                .groupByKey(Grouped.with(Serdes.String(), productAggregationDtoSerde))
                .windowedBy(TimeWindows
                        .of(Duration.of(windowDuration, MILLIS))
                        .grace(Duration.of(graceDuration, MILLIS))
                )
                .reduce((previous, next) -> next)
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                .to("product-aggregation-topic", Produced.with(Serdes.String(), productAggregationDtoSerde));
        return builder.stream("product-aggregation-topic", Consumed.with(Serdes.String(), productAggregationDtoSerde));
    }

    private KStream<String, SellerAggregationDto> buildSellerAggregationCostKStream(StreamsBuilder builder) {
        productAggregationDtoKStream
                .map((key, value) -> {
                    SellerAggregationDto sellerAggregationDto = SellerAggregationDto.builder()
                            .displayDate(value.getDisplayDate())
                            .sellerId(value.getSellerId())
                            .impressionCount(value.getImpressionCount())
                            .clickCount(value.getClickCount())
                            .build();
                    return new KeyValue<>(sellerAggregationDto.getAggregationId(), sellerAggregationDto);
                })
                .groupByKey(Grouped.with(Serdes.String(), sellerAggregationDtoSerde))
                .reduce(SellerAggregationDto::add, Materialized.as("seller-aggregation-table"))
                .toStream()
                .groupByKey(Grouped.with(Serdes.String(), sellerAggregationDtoSerde))
                .windowedBy(TimeWindows
                        .of(Duration.of(windowDuration, MILLIS))
                        .grace(Duration.of(graceDuration, MILLIS))
                )
                .reduce((previous, next) -> next)
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                .to("seller-aggregation-topic", Produced.with(Serdes.String(), sellerAggregationDtoSerde));
        return builder.stream("seller-aggregation-topic", Consumed.with(Serdes.String(), sellerAggregationDtoSerde));
    }

    private void notifyExcessImpression() {
        sellerAggregationDtoKStream
                .filter((key, value) -> value.getImpressionCount() > 10L)
                .foreach((key, value) -> log.info(value.toString()));
    }
}
