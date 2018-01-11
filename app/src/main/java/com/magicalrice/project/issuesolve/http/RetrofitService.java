package com.magicalrice.project.issuesolve.http;


import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


/**
 * Created by Adolph on 2018/1/10.
 */

public interface RetrofitService {
    @FormUrlEncoded
    @POST("get_result")
    Observable<ResultRes> getResult(@Field("issue") String issue);
}
