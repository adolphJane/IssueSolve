package com.magicalrice.project.issuesolve.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Adolph on 2018/1/8.
 */

public class SimpleUtil {

    public static int dp2px(Context context,int value){
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    /**
     * 文件转base64字符串
     *
     * @param file
     * @return
     */
    public static String fileToBase64(File file) {
        String base64 = null;
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return base64;
    }
}
