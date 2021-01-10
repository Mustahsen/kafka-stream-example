package com.mergen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;

    private Date displayDate;

    private String channel;

    private List<EventDataDto> data;

    private Date timestamp;

}
