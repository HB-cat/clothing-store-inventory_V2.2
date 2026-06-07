package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.BuyerPersonalInfo;
import com.heben.clothing_s_i_server.service.BuyerInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerInfoController {

    private final BuyerInfoService buyerInfoService;

    @GetMapping("/info")
    public ApiResponse<BuyerPersonalInfo> getInfo(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role) {
        if (role != 3) return ApiResponse.forbidden("仅买家可访问");
        BuyerPersonalInfo info = buyerInfoService.getInfo(userId);
        return ApiResponse.success(info != null ? info : new BuyerPersonalInfo());
    }

    @PutMapping("/info")
    public ApiResponse<Void> saveInfo(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @RequestBody Map<String, Object> body) {
        if (role != 3) return ApiResponse.forbidden("仅买家可访问");
        BuyerPersonalInfo info = new BuyerPersonalInfo();
        if (body.get("age") != null) info.setAge(Integer.parseInt(body.get("age").toString()));
        if (body.get("height") != null) info.setHeight(Integer.parseInt(body.get("height").toString()));
        if (body.get("weight") != null) info.setWeight(new BigDecimal(body.get("weight").toString()));
        if (body.get("bust") != null) info.setBust(new BigDecimal(body.get("bust").toString()));
        if (body.get("waist") != null) info.setWaist(new BigDecimal(body.get("waist").toString()));
        if (body.get("hip") != null) info.setHip(new BigDecimal(body.get("hip").toString()));
        if (body.get("stylePreference") != null) info.setStylePreference(body.get("stylePreference").toString());
        buyerInfoService.saveInfo(userId, info);
        return ApiResponse.success("保存成功", null);
    }
}
