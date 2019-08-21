package com.magicalrice.project.issuesolve.http;


import com.magicalrice.project.issuesolve.Bean.BaiduTokenBean;
import com.magicalrice.project.issuesolve.Bean.OCRResultBean;
import com.magicalrice.project.issuesolve.Bean.ResultBean;
import com.magicalrice.project.issuesolve.config.HttpConfig;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;


/**
 * Created by Adolph on 2018/1/10.
 */

public interface RetrofitService {
    @FormUrlEncoded
    @POST(HttpConfig.METHOD_GET_RESULT)
    Observable<ResultBean> getResult(@Field("issue") String issue);

    @FormUrlEncoded
    @POST(HttpConfig.METHOD_OCR)
    Observable<OCRResultBean> getOCR(@Query("access_token") String access_token,
                                     @Field("image") String baseImg,
                                     @Field("language_type") String language);

    @FormUrlEncoded
    @POST(HttpConfig.METHOD_TOKEN)
    Observable<BaiduTokenBean> getToken(@Field("grant_type") String type,
                                        @Field("client_id") String client_id,
                                        @Field("client_secret") String client_secret
    );
}
