package com.heben.clothing_s_i_server.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AgentParseResponse {
    private String intent;
    private Map<String, Object> entities;
    private String rawText;
    private Double confidence;
    private String errorMessage;
}
