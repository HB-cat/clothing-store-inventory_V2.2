package com.heben.clothing_s_i_server.dto;

import lombok.Data;

@Data
public class SlowMovingProduct {
    private Long productId;
    private String productName;
    private Long combinationId;
    private String attrsDesc;
    private int stock;
    private int daysWithoutSale;
}
