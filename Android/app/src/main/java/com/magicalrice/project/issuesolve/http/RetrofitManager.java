package com.magicalrice.project.issuesolve.http;

import com.magicalrice.project.issuesolve.config.HttpConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Adolph on 2018/1/10.
 */

public class RetrofitManager {
    private static RetrofitManager instance = null;
    private RetrofitService service;

    public synchronized static RetrofitManager getInstance(String baseUrl) {
        return new RetrofitManager(baseUrl);
    }

    private RetrofitManager(String baseUrl) {
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        service = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(RetrofitService.class);
    }

    public RetrofitService getService() {
        return service;
    }
}
