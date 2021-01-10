package com.mergen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDataDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sellerId;
    private Long productId;

}
