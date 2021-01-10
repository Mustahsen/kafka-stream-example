package com.mergen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlattenedEventDto implements Aggregation, Serializable {

    private static final long serialVersionUID = 1L;

    private Date displayDate;
    private Long sellerId;
    private Long productId;
    private String channel;
    private long impressionCount;
    private long clickCount;

    @Override
    public String getAggregationId() {
        return displayDate.getTime() + sellerId.toString() + productId.toString() +  channel;
    }

    public FlattenedEventDto add(FlattenedEventDto that) {
        this.impressionCount = this.impressionCount + that.impressionCount;
        this.clickCount = this.clickCount + that.clickCount;
        return this;
    }

}

