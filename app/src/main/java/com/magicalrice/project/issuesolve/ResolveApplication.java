package com.magicalrice.project.issuesolve;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;
import com.magicalrice.project.issuesolve.config.SpeechConfig;

/**
 * Created by Adolph on 2018/1/11.
 */

public class ResolveApplication extends Application {

    private boolean isShowResult;
    private static ResolveApplication application;

    public static ResolveApplication getInstance() {
        return application;
    }

    public boolean isShowResult() {
        return isShowResult;
    }

    public void setShowResult(boolean showResult) {
        isShowResult = showResult;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        SpeechUtility.createUtility(getApplicationContext(), "appid=" + SpeechConfig.APPID);
    }
}
