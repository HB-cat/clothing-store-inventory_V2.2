package com.heben.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:8080/"; // 模拟器 localhost
    private static Retrofit retrofit = null;

    public static ApiService getService() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                        private static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
                        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_PATTERN);

                        @Override
                        public void write(JsonWriter out, LocalDateTime value) throws IOException {
                            out.value(value != null ? value.format(formatter) : null);
                        }

                        @Override
                        public LocalDateTime read(JsonReader in) throws IOException {
                            String str = in.nextString();
                            if (str == null || str.isEmpty()) return null;
                            // 兼容结尾带 Z 或 +08:00 的情况
                            if (str.length() > 19) str = str.substring(0, 19);
                            return LocalDateTime.parse(str, formatter);
                        }
                    })
                    .setLenient()
                    .create();

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = chain -> {
                String token = com.heben.client.util.TokenManager.getToken();
                okhttp3.Request.Builder builder = chain.request().newBuilder();
                if (token != null && !token.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }
                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
