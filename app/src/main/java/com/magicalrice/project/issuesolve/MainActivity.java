package com.magicalrice.project.issuesolve;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private Button openAssist;
    private ImageView imgScreenCapture;
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
    private Bitmap mBitmap;
    private String mImagePath, mImageName;
    private RxPermissions rxPer;
    private String token;
    private StringBuilder identifiedTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createEnvironment();
        showPanel();
    }

    private void showPanel() {
        final ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.ico_search);

        openAssist = findViewById(R.id.btn_open);
        imgScreenCapture = findViewById(R.id.screen_capture);
        openAssist.setOnClickListener(v -> {
            if (!isShow) {
                FloatWindow.with(getApplicationContext())
                        .setDesktopShow(true)
                        .setView(img)
                        .setWidth(Screen.width, 0.15f)
                        .setHeight(Screen.height, 0.15f)
                        .setY(Screen.height, 0.6f)
                        .setDesktopShow(true)
                        .setMoveType(MoveType.slide)
                        .setTag("subject")
                        .build();
                onResume();
                ((Button) v).setText("Close Assist");
            } else {
                FloatWindow.get("subject").hide();
                FloatWindow.destroy("subject");
                ((Button) v).setText("Open Assist");
            }

            isShow = !isShow;
        });

        img.setOnClickListener(v -> {
            startScreenCapture();
        });
    }

    private void createEnvironment() {
        mImagePath = Environment.getExternalStorageDirectory().getPath() + "/magic/screenshot/";
        mpm = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mWindowWidth = metrics.widthPixels;
        mWindowHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(mWindowWidth, mWindowHeight, 0x1, 2);
        rxPer = new RxPermissions(this);

        OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                token = accessToken.getAccessToken();
            }

            @Override
            public void onError(OCRError ocrError) {

            }
        },getApplicationContext());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(MainActivity.this, "Failed to get capture!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (this == null) {
                return;
            }

            mResultCode = resultCode;
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
            new Handler().postDelayed(() -> {

            }, 500);
        }
    }

    private void setUpMediaProjection() {
        mediaProjection = mpm.getMediaProjection(mResultCode, mResultData);
    }

    private void setUpVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", mWindowWidth, mWindowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    private void startScreenCapture() {
        mImageName = System.currentTimeMillis() + ".png";
        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        mBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(buffer);
        Matrix matrix = new Matrix();
        matrix.postScale(0.5f, 0.5f);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);

        image.close();

        stopScreenCapture();
        if (mBitmap != null) {
            imgScreenCapture.setImageBitmap(mBitmap);
        }

        checkPersimission();
    }

    private void stopScreenCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private void checkPersimission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!rxPer.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                rxPer.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted){
                                saveToFile();
                            } else {
                                Toast.makeText(this.getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                saveToFile();
            }
        } else {
            saveToFile();
        }


    }

    private void saveToFile() {

        try {
            File fileFolder = new File(mImagePath);
            if (!fileFolder.exists())
                fileFolder.mkdirs();
            File file = new File(mImagePath, mImageName);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            out.flush();
            out.close();
            Toast.makeText(this.getApplicationContext(), "Screenshot is done.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void transformImage(){
        if (mBitmap == null){
            return;
        }

        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();

    }

    private void ocrSubject(){
        identifiedTitle = new StringBuilder();
        GeneralBasicParams params = new GeneralBasicParams();
        params.setDetectDirection(true);
        params.setImageFile(new File(mImagePath,mImageName));
        OCR.getInstance().recognizeGeneralBasic(params, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult generalResult) {
                for (WordSimple wordSimple : generalResult.getWordList()){
                    identifiedTitle.append(wordSimple.getWords());
                    identifiedTitle.append("\n");
                    Toast.makeText(MainActivity.this,identifiedTitle,Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(OCRError ocrError) {

            }
        });
    }
}
