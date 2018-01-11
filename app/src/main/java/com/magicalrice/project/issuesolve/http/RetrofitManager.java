package com.magicalrice.project.issuesolve.http;

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

    public synchronized static RetrofitManager getInstance() {
        return instance != null ? instance : new RetrofitManager();
    }

    private static OkHttpClient client = new OkHttpClient()
            .newBuilder()
            .addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY))
            .build();
    private static RetrofitService retrofitService = new Retrofit.Builder()
            .baseUrl("http://47.100.24.16:4300/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(RetrofitService.class);

    public static RetrofitService getService() {
        return retrofitService;
    }
}
