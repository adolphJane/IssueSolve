package com.magicalrice.project.issuesolve.utils;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.magicalrice.project.issuesolve.R;
import com.magicalrice.project.issuesolve.ResolveApplication;

import java.io.ObjectInputStream;
import java.lang.reflect.Field;

/**
 * Created by Adolph on 2018/1/15.
 */

public class CToast {
    public static final int CTOAST_LONG = 10 * 1000;
    public static final int CTOAST_SHORT = 1 * 1000;

    private static CToast instance;
    public Toast toast;
    private int toastDur = CTOAST_SHORT;

    public void showToast(String title, String message) {
        showToastBottom(title, message);
    }

    private void showToastBottom(String title, String message) {
        if (toast == null) {
            View view = LayoutInflater.from(ResolveApplication.getInstance().getApplicationContext()).inflate(R.layout.c_toast, null);
            toast = new Toast(ResolveApplication.getInstance().getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);
            toast.setView(view);
            try {
                Object mTN;
                mTN = getField(toast, "mTN");
                if (mTN != null) {
                    Object mParams = getField(mTN, "mParams");
                    if (mParams != null && mParams instanceof WindowManager.LayoutParams) {
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mParams;
                        params.windowAnimations = R.style.CToast;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        TextView tvTitle = toast.getView().findViewById(R.id.title);
        TextView tvMessage = toast.getView().findViewById(R.id.message);
        tvTitle.setText(title);
        tvMessage.setText(message);
        toast.setDuration(toastDur);
        toast.show();
    }

    public static CToast getInstance() {
        if (instance == null) {
            synchronized (CToast.class) {
                if (instance == null) {
                    instance = new CToast();
                }
            }
        }
        return instance;
    }

    public CToast setDuration(int dur) {
        toastDur = dur;
        return this;
    }

    /**
     * 反射字段
     *
     * @param object    要反射的对象
     * @param fieldName 要反射的字段名称
     * @return
     */
    private static Object getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        if (field != null) {
            field.setAccessible(true);
            return field.get(object);
        }
        return null;
    }
}
