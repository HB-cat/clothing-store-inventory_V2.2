package com.heben.client.model;

import java.util.List;
import java.util.Map;

public class Models {

    // ===== Auth =====
    public static class LoginRequest {
        public String username, password;
        public LoginRequest(String username, String password) { this.username = username; this.password = password; }
    }

    public static class RegisterRequest {
        public String username, password, shopName, nickname;
        public int role;
    }

    public static class LoginResponse {
        public String token, username, shopName, nickname;
        public Long userId;
        public int role;
    }

    public static class ChangePasswordRequest {
        public String oldPassword, newPassword;
    }

    public static class User {
        public Long id;
        public String username, shopName, nickname, avatarUrl;
        public int role, status;
    }

    // ===== Category =====
    public static class Category {
        public Long id;
        public String name;
        public int sortOrder;
        public boolean isPreset;
    }

    // ===== Attribute =====
    public static class AttributeGroup {
        public Long id;
        public String name;
        public boolean isPreset;
        public List<AttributeValue> values;
    }

    public static class AttributeValue {
        public Long id, groupId;
        public String value;
        public int sortOrder;
    }

    // ===== Product =====
    public static class Product {
        public Long id;
        public String name;
        public Long categoryId;
        public String categoryName;
        public double purchasePrice, sellingPrice;
        public String imageUrl;
        public boolean hasAttributes;
        public List<ProductAttrCombination> combinations;
    }

    public static class ProductAttrCombination {
        public Long id, productId;
        public String attrsJson, attrsDisplay;
        public int stock, version;
        public double sellingPrice;
    }

    // ===== Sales =====
    public static class SaleRecord {
        public Long id;
        public String buyerName;
        public double totalAmount;
        public int itemCount;
        public String saleTime;
        public List<SaleRecordItem> items;
    }

    public static class SaleRecordItem {
        public Long id, saleRecordId, combinationId;
        public String productName, attrsDisplay;
        public int quantity;
        public double unitPrice, subtotal;
    }

    public static class SalesSummary {
        public double totalAmount, totalProfit;
        public int totalCount;
        public List<SaleRecord> records;
    }

    // ===== Draft Bill =====
    public static class DraftBill {
        public Long id;
        public String draftNo;
        public double totalAmount;
        public int itemCount;
        public String createdAt;
        public List<DraftBillItem> items;
    }

    public static class DraftBillItem {
        public Long id, draftBillId, combinationId;
        public String productName, attrsDisplay;
        public int quantity;
        public double unitPrice;
    }

    // ===== Purchase =====
    public static class PurchaseRecord {
        public Long id;
        public double totalAmount;
        public String createdAt;
    }

    public static class SharedProduct {
        public Long id, productId, wholesalerId;
        public String productName, wholesalerName;
        public double sharedPrice;
        public int stock;
    }

    // ===== Wholesale Transaction =====
    public static class WholesaleTransaction {
        public Long id;
        public int status;
    }

    // ===== Chat =====
    public static class ChatSession {
        public Long id;
        public String otherName, otherAvatar;
        public String lastMessage;
        public String lastTime;
        public int unreadCount;
    }

    public static class ChatMessage {
        public Long id, sessionId, senderId;
        public String content, messageType;
        public String createdAt;
        public boolean isMine;
    }

    // ===== Appointment =====
    public static class Appointment {
        public Long id, buyerId;
        public String buyerName, productName;
        public String timeSlot, status;
        public int visitCount, cancelCount;
    }

    // ===== Analytics =====
    public static class TodayReport {
        public double totalAmount, totalProfit;
        public int orderCount;
        public String conclusion;
    }

    public static class LowStockProduct {
        public String productName, attrsDisplay;
        public int stock;
        public boolean isEmergency;
    }

    public static class SlowMovingProduct {
        public String productName;
        public int stock, sales30Day;
    }

    public static class TopProduct {
        public String productName;
        public int salesCount;
        public double salesAmount;
    }

    public static class DailyTrend {
        public String date;
        public double amount;
    }

    public static class PeakHour {
        public int hour;
        public int orderCount;
    }

    public static class CustomerProfile {
        public String summary;
        public List<TagItem> tags;
    }

    public static class TagItem {
        public String label, value;
    }

    // ===== Agent =====
    public static class AgentParseResponse {
        public String action, confidence;
        public Map<String, Object> params;
        public String displayText;
    }
}
