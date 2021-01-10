package com.mergen.configuration;

import com.mergen.configuration.TopicConfigurations.TopicConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TopicAdministrator {

  private final TopicConfigurations configurations;
  private final GenericApplicationContext context;

  @PostConstruct
  public void createTopics() {
    configurations
            .getTopics()
            .ifPresent(this::initializeBeans);
  }

  private void initializeBeans(List<TopicConfiguration> topics) {
    log.info("Configuring {} topics", topics.size());
    topics.forEach(t -> {
      log.info(t.toString());
      context.registerBean(t.getName(), NewTopic.class, t::toNewTopic);
    });
  }

}