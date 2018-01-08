package com.magicalrice.project.issuesolve;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private Button openAssist;
    private boolean isShow = false;
    private static final int REQUEST_MEDIA_PROJECTION = 10002;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mpm;
    private int mResultCode;
    private Intent mResultData;
    private VirtualDisplay virtualDisplay;
    private WindowManager mWindowManager;
    private int mWindowWidth;
    private int mWindowHeight;
    private int mScreenDensity;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createEnvironment();
        showPanel();
    }

    private void showPanel(){
        final View view = View.inflate(this,R.layout.float_view,null);
        openAssist = findViewById(R.id.btn_open);
        openAssist.setOnClickListener(v -> {
            if (!isShow){
                FloatWindow.with(getApplicationContext())
                        .setDesktopShow(true)
                        .setView(view)
                        .setHeight(Screen.height,0.38f)
                        .setWidth(Screen.width,0.85f)
                        .setX(SimpleUtil.dp2px(MainActivity.this,30))
                        .setY(SimpleUtil.dp2px(MainActivity.this,105))
                        .setDesktopShow(true)
                        .setMoveType(MoveType.inactive)
                        .setTag("subject")
                        .build();
                onResume();
                ((Button)v).setText("Close Assist");
            } else {
                FloatWindow.get("subject").hide();
                FloatWindow.destroy("subject");
                ((Button)v).setText("Open Assist");
            }

            isShow = !isShow;
        });

        view.setOnClickListener(v -> {
        });
    }

    private void createEnvironment(){
        mpm = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(),REQUEST_MEDIA_PROJECTION);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mWindowWidth = metrics.widthPixels;
        mWindowHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(mWindowWidth,mWindowHeight,0x1,2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION){
            if (resultCode != Activity.RESULT_OK){
                Toast.makeText(MainActivity.this,"Failed to get capture!",Toast.LENGTH_SHORT).show();
                return;
            }
            if (this == null){
                return;
            }

            mResultCode = resultCode;
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
            new Handler().postDelayed(()->{

            },500);
        }
    }

    private void setUpMediaProjection(){
        mediaProjection = mpm.getMediaProjection(mResultCode,mResultData);
    }

    private void setUpVirtualDisplay(){
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",mWindowWidth,mWindowHeight,mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mImageReader.getSurface(),null,null);
    }

    private void startScreenCapture(){
        Image image = mImageReader.acquireLatestImage();
        if (image == null){
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;


    }
}
