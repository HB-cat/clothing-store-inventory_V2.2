package com.heben.client.network;

import com.heben.client.model.*;
import com.heben.client.model.Models.*;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ===== Auth =====
    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<ApiResponse<LoginResponse>> register(@Body RegisterRequest request);

    @POST("api/auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    // ===== Categories =====
    @GET("api/categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @POST("api/categories")
    Call<ApiResponse<Category>> addCategory(@Body Map<String, String> body);

    @PUT("api/categories/{id}")
    Call<ApiResponse<Void>> updateCategory(@Path("id") Long id, @Body Map<String, String> body);

    @DELETE("api/categories/{id}")
    Call<ApiResponse<Void>> deleteCategory(@Path("id") Long id);

    @PUT("api/categories/sort")
    Call<ApiResponse<Void>> sortCategories(@Body Map<String, List<Long>> body);

    // ===== Attribute Groups =====
    @GET("api/attribute-groups")
    Call<ApiResponse<List<AttributeGroup>>> getAttributeGroups();

    @POST("api/attribute-groups")
    Call<ApiResponse<AttributeGroup>> addAttributeGroup(@Body Map<String, String> body);

    @DELETE("api/attribute-groups/{id}")
    Call<ApiResponse<Void>> deleteAttributeGroup(@Path("id") Long id);

    @POST("api/attribute-values")
    Call<ApiResponse<AttributeValue>> addAttributeValue(@Body Map<String, Object> body);

    @DELETE("api/attribute-values/{id}")
    Call<ApiResponse<Void>> deleteAttributeValue(@Path("id") Long id);

    // ===== Products =====
    @GET("api/products")
    Call<ApiResponse<List<Product>>> getProducts(
            @Query("categoryId") Long categoryId,
            @Query("keyword") String keyword
    );

    @POST("api/products")
    Call<ApiResponse<Product>> addProduct(@Body Map<String, Object> body);

    @PUT("api/products/{id}")
    Call<ApiResponse<Void>> updateProduct(@Path("id") Long id, @Body Map<String, Object> body);

    @DELETE("api/products/{id}")
    Call<ApiResponse<Void>> deleteProduct(@Path("id") Long id);

    // ===== Sales =====
    @POST("api/sales/quick")
    Call<ApiResponse<SaleRecord>> quickSale(@Body Map<String, Object> body);

    @POST("api/sales/draft")
    Call<ApiResponse<DraftBill>> createDraft(@Body Map<String, Object> body);

    @GET("api/sales/drafts")
    Call<ApiResponse<List<DraftBill>>> getDrafts();

    @PUT("api/sales/draft/{id}")
    Call<ApiResponse<Void>> updateDraft(@Path("id") Long id, @Body Map<String, Object> body);

    @POST("api/sales/draft/{id}/submit")
    Call<ApiResponse<Void>> submitDraft(@Path("id") Long id);

    @DELETE("api/sales/draft/{id}")
    Call<ApiResponse<Void>> deleteDraft(@Path("id") Long id);

    @GET("api/sales/today")
    Call<ApiResponse<SalesSummary>> getTodaySales();

    @GET("api/sales/month")
    Call<ApiResponse<SalesSummary>> getMonthSales();

    @GET("api/sales/year")
    Call<ApiResponse<SalesSummary>> getYearSales();

    @GET("api/sales/records")
    Call<ApiResponse<List<SaleRecord>>> getSalesRecords(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @POST("api/sales/return")
    Call<ApiResponse<Void>> returnSale(@Body Map<String, Object> body);

    // ===== Purchases =====
    @POST("api/purchases/my-products")
    Call<ApiResponse<PurchaseRecord>> purchaseMyProducts(@Body Map<String, Object> body);

    @POST("api/purchases/new-product")
    Call<ApiResponse<PurchaseRecord>> purchaseNewProduct(@Body Map<String, Object> body);

    @POST("api/purchases/wholesale-request")
    Call<ApiResponse<Object>> wholesaleRequest(@Body Map<String, Object> body);

    @PUT("api/purchases/wholesale-confirm/{id}")
    Call<ApiResponse<Void>> wholesaleConfirm(@Path("id") Long id, @Body Map<String, Object> body);

    // ===== Shared Products =====
    @GET("api/shared-products/available")
    Call<ApiResponse<List<SharedProduct>>> getAvailableSharedProducts(
            @Query("keyword") String keyword
    );

    @POST("api/shared-products/share/{productId}")
    Call<ApiResponse<Void>> shareProduct(@Path("productId") Long productId, @Body Map<String, Object> body);

    @DELETE("api/shared-products/unshare/{productId}")
    Call<ApiResponse<Void>> unshareProduct(@Path("productId") Long productId);

    // ===== Analytics =====
    @GET("api/analytics/today")
    Call<ApiResponse<TodayReport>> getTodayReport();

    @GET("api/analytics/low-stock")
    Call<ApiResponse<List<LowStockProduct>>> getLowStockProducts();

    @GET("api/analytics/slow-moving")
    Call<ApiResponse<List<SlowMovingProduct>>> getSlowMovingProducts();

    @GET("api/analytics/top-products")
    Call<ApiResponse<List<TopProduct>>> getTopProducts();

    @GET("api/analytics/trend")
    Call<ApiResponse<List<DailyTrend>>> getSalesTrend();

    @GET("api/analytics/peak-hours")
    Call<ApiResponse<List<PeakHour>>> getPeakHours();

    @GET("api/analytics/customer-profile")
    Call<ApiResponse<CustomerProfile>> getCustomerProfile();

    // ===== Chat =====
    @GET("api/chat/sessions")
    Call<ApiResponse<List<ChatSession>>> getChatSessions();

    @GET("api/chat/messages/{sessionId}")
    Call<ApiResponse<List<ChatMessage>>> getChatMessages(@Path("sessionId") Long sessionId);

    @POST("api/chat/send")
    Call<ApiResponse<Void>> sendMessage(@Body Map<String, Object> body);

    // ===== Appointments =====
    @GET("api/appointments")
    Call<ApiResponse<List<Appointment>>> getAppointments(@Query("status") String status);

    @PUT("api/appointments/{id}/confirm")
    Call<ApiResponse<Void>> confirmAppointment(@Path("id") Long id);

    @PUT("api/appointments/{id}/reject")
    Call<ApiResponse<Void>> rejectAppointment(@Path("id") Long id, @Body Map<String, String> body);

    @PUT("api/appointments/{id}/complete")
    Call<ApiResponse<Void>> completeAppointment(@Path("id") Long id);

    // ===== Agent =====
    @POST("api/agent/parse")
    Call<ApiResponse<AgentParseResponse>> parseAgentCommand(@Body Map<String, String> body);
}
