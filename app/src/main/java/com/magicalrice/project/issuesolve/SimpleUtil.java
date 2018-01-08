package com.magicalrice.project.issuesolve;

import android.content.Context;

/**
 * Created by Adolph on 2018/1/8.
 */

public class SimpleUtil {

    public static int dp2px(Context context,int value){
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }
}
