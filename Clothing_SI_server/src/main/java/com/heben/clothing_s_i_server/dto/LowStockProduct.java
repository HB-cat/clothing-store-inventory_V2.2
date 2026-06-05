package com.heben.clothing_s_i_server.dto;

import lombok.Data;

@Data
public class LowStockProduct {
    private Long combinationId;
    private Long productId;
    private String productName;
    private String attrsDesc;
    private int stock;
}
