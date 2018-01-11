package com.magicalrice.project.issuesolve.http;


import com.magicalrice.project.issuesolve.Bean.ResultBean;
import com.magicalrice.project.issuesolve.config.HttpConfig;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


/**
 * Created by Adolph on 2018/1/10.
 */

public interface RetrofitService {
    @FormUrlEncoded
    @POST(HttpConfig.METHOD_GET_RESULT)
    Observable<ResultBean> getResult(@Field("issue") String issue);
}
