package com.magicalrice.project.issuesolve;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.magicalrice.project.issuesolve.Bean.ResultBean;
import com.magicalrice.project.issuesolve.Bean.SpeechBean;
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
    private StringBuilder identifiedTitle, answer1, answer2, answer3;
    private SpeechRecognizer recognizer;
    private Gson gson;
    private boolean hasQes, hasAnswer1, hasAnswer2, hasAnswer3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createEnvironment();
        showPanel();
    }

    private void showPanel() {
        final View view = View.inflate(getApplicationContext(), R.layout.control_view, null);
        ImageView img_search = view.findViewById(R.id.btn_search);
        ImageView img_mic = view.findViewById(R.id.btn_mic);

        openAssist = findViewById(R.id.btn_open);
        imgScreenCapture = findViewById(R.id.screen_capture);
        openAssist.setOnClickListener(v -> {
            if (checkPersimission()) {
                if (!isShow) {
                    FloatWindow.with(getApplicationContext())
                            .setDesktopShow(true)
                            .setView(view)
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

        img_search.setOnClickListener(v -> {
            if (canScreenCapture()) {
                new Handler().postDelayed(() -> {
                    startScreenCapture();
                }, 200);
            }
        });

        img_mic.setOnClickListener(v -> {
            if (!recognizer.isListening()) {
                startSpeech();
            } else {

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
        gson = new Gson();
        isShow = (boolean) SPUtil.getInstance(this).get("isShowController", false);
        OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {

            }

            @Override
            public void onError(OCRError ocrError) {

            }
        }, getApplicationContext());

        recognizer = SpeechRecognizer.createRecognizer(getApplicationContext(), null);
        // 语音识别应用领域（：iat，search，video，poi，music）
        recognizer.setParameter(SpeechConstant.DOMAIN, "iat");
        // 接收语言中文
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 接受的语言是普通话
        recognizer.setParameter(SpeechConstant.ACCENT, "mandarin ");
        // 设置听写引擎（云端）
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        recognizer.setParameter(SpeechConstant.VOLUME, "80");
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

    private void startSpeech() {
        recognizer.startListening(mRecognizer);

    }

    private RecognizerListener mRecognizer = new com.iflytek.cloud.RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
        }

        @Override
        public void onBeginOfSpeech() {
            identifiedTitle = new StringBuilder();
        }

        @Override
        public void onEndOfSpeech() {
            Log.e("sound_test", identifiedTitle.toString());
            showResult(identifiedTitle.toString());
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            SpeechBean bean = gson.fromJson(recognizerResult.getResultString(), SpeechBean.class);
            Log.e("sound", bean.toString());
            identifiedTitle.append(bean.toString());
        }

        @Override
        public void onError(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

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
                rxPer.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
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

    private void initOCR() {
        hasQes = false;
        hasAnswer1 = false;
        hasAnswer2 = false;
        hasAnswer3 = false;
        identifiedTitle = new StringBuilder();
        answer1 = new StringBuilder();
        answer2 = new StringBuilder();
        answer3 = new StringBuilder();
    }

    private void ocrSubject() {
        initOCR();
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
                        hasQes = true;
                        showResult(issue);
                        break;
                    }
                    if (hasQes) {
                        answer1.append(wordSimple.getWords());
                        hasAnswer1 = true;
                    }
                    if (hasAnswer1) {
                        answer2.append(wordSimple.getWords());
                        hasAnswer2 = true;
                    }
                    if (hasAnswer2) {
                        answer3.append(wordSimple.getWords());
                        hasAnswer3 = true;
                    }
                    if (hasAnswer3) {
                        return;
                    }
                }
            }

            @Override
            public void onError(OCRError ocrError) {

            }
        });
    }

    private void showResult(String issue) {
        Log.e("issue", issue);
        RetrofitManager.getInstance()
                .getService()
                .getResult(issue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResultBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResultBean resultBean) {
                        if (resultBean.getResult().contains(answer1)) {
                            Toast.makeText(getApplicationContext(), answer1, Toast.LENGTH_LONG).show();
                        } else if (resultBean.getResult().contains(answer2)) {
                            Toast.makeText(getApplicationContext(), answer2, Toast.LENGTH_LONG).show();
                        } else if (resultBean.getResult().contains(answer3)) {
                            Toast.makeText(getApplicationContext(), answer3, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), resultBean.getResult(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "It`s error", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

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
