package com.magicalrice.project.issuesolve;

import android.content.Intent;

/**
 * Created by Adolph on 2018/1/21.
 */

public interface MainPresenter {

    void showSearchBtn();

    void startCapture();

    void startMic();

    void onDestroy();

    void createEnvironment();

    boolean checkPersimission();

    void resetCapture(int code, Intent data);

    void clearCache();
}
