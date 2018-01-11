package com.magicalrice.project.issuesolve;

import android.app.Application;

/**
 * Created by Adolph on 2018/1/11.
 */

public class ResolveApplication extends Application {

    private boolean isShowResult;
    private static ResolveApplication application;

    public static ResolveApplication getInstance() {
        return application == null ? new ResolveApplication() : application;
    }

    public boolean isShowResult() {
        return isShowResult;
    }

    public void setShowResult(boolean showResult) {
        isShowResult = showResult;
    }
}
