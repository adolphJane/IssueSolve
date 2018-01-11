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
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.magicalrice.project.issuesolve.http.ResultRes;
import com.magicalrice.project.issuesolve.http.RetrofitManager;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
        img.setImageResource(R.drawable.btn_search_layer_list);

        openAssist = findViewById(R.id.btn_open);
        imgScreenCapture = findViewById(R.id.screen_capture);
        openAssist.setOnClickListener(v -> {
            if (checkPersimission()) {
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
                    if (FloatWindow.get("subject") != null) {
                        FloatWindow.get("subject").hide();
                        FloatWindow.destroy("subject");
                        ((Button) v).setText("Open Assist");
                    }
                }

                isShow = !isShow;
            }
        });

        img.setOnClickListener(v -> {
            if (canScreenCapture()) {
                new Handler().postDelayed(() -> {
                    startScreenCapture();
                }, 200);
            }
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
        isShow = (boolean) SPUtil.getInstance(this).get("isShowController", false);
        OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                token = accessToken.getAccessToken();
            }

            @Override
            public void onError(OCRError ocrError) {

            }
        }, getApplicationContext());
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
        }
    }

    private void setUpMediaProjection() {
        mediaProjection = mpm.getMediaProjection(mResultCode, mResultData);
    }

    private void setUpVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", mWindowWidth, mWindowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    private boolean canScreenCapture() {
        if (this == null) {
            return false;
        }
        if (mediaProjection != null) {
            setUpVirtualDisplay();
            return true;
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
            return true;
        } else {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            return false;
        }
    }

    private void startScreenCapture() {
        mImageName = System.currentTimeMillis() + ".jpg";
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
        mBitmap = Bitmap.createBitmap(mBitmap, 0, (int) (height * 0.1), width, (int) (height * 0.5), matrix, true);

        image.close();

        stopScreenCapture();
        if (mBitmap != null) {
            imgScreenCapture.setImageBitmap(mBitmap);
        }

        saveToFile();
    }

    private void stopScreenCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private boolean checkPersimission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!rxPer.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                rxPer.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted) {
                                Toast.makeText(this.getApplicationContext(), "Permission accessed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this.getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                            }
                        });
                return false;
            } else {
                return true;
            }
        } else {
            return false;
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
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(this.getApplicationContext(), "Screenshot is done.", Toast.LENGTH_SHORT).show();

            ocrSubject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ocrSubject() {
        identifiedTitle = new StringBuilder();
        GeneralBasicParams params = new GeneralBasicParams();
        params.setDetectDirection(true);
        params.setImageFile(new File(mImagePath, mImageName));
        OCR.getInstance().recognizeGeneralBasic(params, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult generalResult) {
                for (WordSimple wordSimple : generalResult.getWordList()) {
                    identifiedTitle.append(wordSimple.getWords());
                    if (wordSimple.getWords().contains("?")) {
                        String issue = identifiedTitle.substring(identifiedTitle.indexOf(".") + 1, identifiedTitle.indexOf("?"));
                        showResult(issue);
                        break;
                    }

                }
            }

            @Override
            public void onError(OCRError ocrError) {

            }
        });
    }

    private void showResult(String issue) {
        RetrofitManager
                .getService()
                .getResult(issue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResultRes>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResultRes resultRes) {
                        handler.removeMessages(1021);
                        if (!ResolveApplication.getInstance().isShowResult()) {
                            View view = View.inflate(MainActivity.this, R.layout.float_view, null);
                            TextView tv = view.findViewById(R.id.tv_result);
                            tv.setText(resultRes.getResult());
                            FloatWindow.with(getApplicationContext())
                                    .setWidth(Screen.width, 1f)
                                    .setHeight(Screen.height, 0.4f)
                                    .setX(0)
                                    .setY(Screen.height, 0.6f)
                                    .setDesktopShow(true)
                                    .setView(view)
                                    .setMoveType(MoveType.inactive)
                                    .setTag("result")
                                    .build();
                            ResolveApplication.getInstance().setShowResult(true);
                        } else {
                            handler.sendEmptyMessage(1021);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "It`s error", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        handler.sendEmptyMessageDelayed(1021, 8000);
                    }
                });
    }

    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1021:
                    if (FloatWindow.get("result") != null) {
                        FloatWindow.get("result").hide();
                        FloatWindow.destroy("result");
                        ResolveApplication.getInstance().setShowResult(false);
                    }
            }
        }
    };

    private void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SPUtil.getInstance(this).put("isShowController", isShow);
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        release();
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
