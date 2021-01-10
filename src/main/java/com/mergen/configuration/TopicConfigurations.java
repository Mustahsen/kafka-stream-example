package com.mergen.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "kafka")
class TopicConfigurations {

    private List<TopicConfiguration> topicConfigs;

    public Optional<List<TopicConfiguration>> getTopics() {
        return Optional.ofNullable(topicConfigs);
    }

    @Getter
    @Setter
    @ToString
    static class TopicConfiguration {
        @NotNull(message = "Topic name is required.")
        private String name;
        private Integer numPartitions = 3;
        private Short replicationFactor = 1;

        NewTopic toNewTopic() {
            return new NewTopic(this.name, this.numPartitions, this.replicationFactor);
        }
    }
}
