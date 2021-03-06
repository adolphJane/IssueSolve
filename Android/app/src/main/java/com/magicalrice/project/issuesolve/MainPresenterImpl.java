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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.magicalrice.project.issuesolve.Bean.BaiduTokenBean;
import com.magicalrice.project.issuesolve.Bean.OCRResultBean;
import com.magicalrice.project.issuesolve.Bean.OCRWordsBean;
import com.magicalrice.project.issuesolve.Bean.ResultBean;
import com.magicalrice.project.issuesolve.Bean.SpeechBean;
import com.magicalrice.project.issuesolve.Bean.SubjectBean;
import com.magicalrice.project.issuesolve.config.HttpConfig;
import com.magicalrice.project.issuesolve.config.OCRConfig;
import com.magicalrice.project.issuesolve.http.RetrofitManager;
import com.magicalrice.project.issuesolve.utils.CToast;
import com.magicalrice.project.issuesolve.utils.SPUtil;
import com.magicalrice.project.issuesolve.utils.SimpleUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yhao.floatwindow.IFloatWindow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Adolph on 2018/1/21.
 */

public class MainPresenterImpl implements MainPresenter, RecognizerListener {
    private static final int REQUEST_MEDIA_PROJECTION = 10002;

    private WindowManager mWindowManager;
    private MainView view;
    private Activity activity;
    private RxPermissions rxPer;
    private String mImagePath, mImageName;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mpm;
    private int mWindowWidth, mWindowHeight, mScreenDensity;
    private ImageReader mImageReader;
    private Gson gson;
    private SpeechRecognizer recognizer;
    private StringBuilder identifiedTitle;
    private SubjectBean bean;
    private String token;
    private VirtualDisplay virtualDisplay;
    private int resultCode;
    private Intent resultData;
    private Bitmap mBitmap;


    public MainPresenterImpl(MainView view, Activity activity) {
        this.view = view;
        this.activity = activity;
    }

    @Override
    public void createEnvironment() {
        rxPer = new RxPermissions(activity);
        mImagePath = Environment.getExternalStorageDirectory().getPath() + "/magic/screenshot/";
        mpm = (MediaProjectionManager) activity.getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        mWindowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mWindowWidth = metrics.widthPixels;
        mWindowHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(mWindowWidth, mWindowHeight, 0x1, 2);
        gson = new Gson();

        initSpeech();
        initOCR();
    }

    @Override
    public void showSearchBtn() {
        if (view != null) {
            view.showSearchButton();
        }
    }

    @Override
    public void startCapture() {
        if (view != null && canScreenCapture()) {
            new Handler().postDelayed(() -> {
                startScreenCapture();
            }, 200);
        }
    }

    @Override
    public void startMic() {
        if (view != null && !recognizer.isListening()) {
            recognizer.startListening(this);
        }
    }

    @Override
    public void onDestroy() {
        releaseVirtualDisplay();
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
    public boolean checkPersimission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!rxPer.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !rxPer.isGranted(Manifest.permission.RECORD_AUDIO)) {
                rxPer.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                        .subscribe(granted -> {
                            if (granted) {
                                Toast.makeText(ResolveApplication.getInstance(), "Permission accessed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ResolveApplication.getInstance(), "Permission denied", Toast.LENGTH_SHORT).show();
                            }
                        });
                if (!rxPer.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !rxPer.isGranted(Manifest.permission.RECORD_AUDIO)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void resetCapture(int code, Intent data) {
        resultCode = code;
        resultData = data;
        mediaProjection = mpm.getMediaProjection(code, data);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", mWindowWidth, mWindowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    @Override
    public void clearCache() {
        File fileFolder = new File(mImagePath);
        if (fileFolder.exists() && fileFolder.isDirectory()) {
            File[] files = fileFolder.listFiles();
            for (File file : files) {
                file.delete();
            }
            if (fileFolder.listFiles().length == 0) {
                Toast.makeText(ResolveApplication.getInstance(), "缓存清除完毕", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ResolveApplication.getInstance(), "缓存清除失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initSpeech() {
        recognizer = SpeechRecognizer.createRecognizer(activity, null);
        // 语音识别应用领域（：iat，search，video，poi，music）
        recognizer.setParameter(SpeechConstant.DOMAIN, "iat");
        // 接收语言中文
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 接受的语言是普通话
        recognizer.setParameter(SpeechConstant.ACCENT, "mandarin ");
        recognizer.setParameter(SpeechConstant.VOLUME, "80");
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
        recognizer.setParameter(SpeechConstant.VAD_BOS, "4000");
    }

    private void initOCR() {
        RetrofitManager.getInstance(HttpConfig.OCR_URL)
                .getService()
                .getToken("client_credentials", OCRConfig.APP_ID, OCRConfig.Secret_Key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaiduTokenBean>() {

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(BaiduTokenBean baiduTokenBean) {
                        token = baiduTokenBean.getAccess_token();
                        Log.e("accessToken", token + "---------token");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onVolumeChanged(int i, byte[] bytes) {

    }

    @Override
    public void onBeginOfSpeech() {
        identifiedTitle = new StringBuilder();
        bean = new SubjectBean();
    }

    @Override
    public void onEndOfSpeech() {
        Log.e("sound_test", identifiedTitle.toString());
        bean.setIssue(identifiedTitle.toString());
        showResult(bean.getIssue());
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

    private boolean canScreenCapture() {
        if (this == null) {
            setUpVirtualDisplay();
            return false;
        }
        if (mediaProjection != null) {
            return true;
        } else if (resultCode != 0 && resultData != null) {
            mediaProjection = mpm.getMediaProjection(resultCode, resultData);
            setUpVirtualDisplay();
            return true;
        } else {
            activity.startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            return false;
        }
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
        mBitmap = Bitmap.createBitmap(mBitmap, 0, (int) (height * 0.1), width, (int) (height * 0.5), matrix, true);

        image.close();

        releaseVirtualDisplay();
        view.showCapture(mBitmap);

        saveToFile();
    }

    private void ocrSubject() {
        bean = new SubjectBean();
        identifiedTitle = new StringBuilder();
        File file = new File(mImagePath, mImageName);
        String base = SimpleUtil.fileToBase64(file);

        RetrofitManager.getInstance(HttpConfig.OCR_URL)
                .getService()
                .getOCR(token, base, "CHN_ENG")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<OCRResultBean>() {

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(OCRResultBean ocrResultBean) {
                        if (ocrResultBean != null) {
                            StringBuilder words = new StringBuilder();
                            for (int i = 0; i < ocrResultBean.getWords_result_num(); i++) {
                                OCRWordsBean oBean = ocrResultBean.getWords_result().get(i);
                                words.append(oBean.getWords());
                                if (oBean.getWords().contains("?")) {
                                    bean.setIssue(words.substring(words.indexOf(".") + 1, words.indexOf("?")));
                                    if (i + 1 < ocrResultBean.getWords_result_num() && ocrResultBean.getWords_result().get(i + 1).getWords() != null) {
                                        bean.setAnswer1(ocrResultBean.getWords_result().get(i + 1).getWords());
                                    }
                                    if (i + 2 < ocrResultBean.getWords_result_num() && ocrResultBean.getWords_result().get(i + 2).getWords() != null) {
                                        bean.setAnswer2(ocrResultBean.getWords_result().get(i + 2).getWords());
                                    }
                                    if (i + 3 < ocrResultBean.getWords_result_num() && ocrResultBean.getWords_result().get(i + 3).getWords() != null) {
                                        bean.setAnswer3(ocrResultBean.getWords_result().get(i + 3).getWords());
                                    }
                                    showResult(bean.getIssue());
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(ResolveApplication.getInstance(), "It`s error", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Log.e("subject", bean.toString());
    }

    private void showResult(String issue) {
        Log.e("issue", issue);
        Log.e("bean", bean.toString());
        RetrofitManager.getInstance(HttpConfig.BASE_URL)
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
                        if (!TextUtils.isEmpty(bean.getAnswer1()) && resultBean.getResult().contains(bean.getAnswer1())) {
                            CToast.getInstance().setDuration(CToast.CTOAST_LONG).showToast(bean.getAnswer1(), resultBean.getResult());
                        } else if (!TextUtils.isEmpty(bean.getAnswer2()) && resultBean.getResult().contains(bean.getAnswer2())) {
                            CToast.getInstance().setDuration(CToast.CTOAST_LONG).showToast(bean.getAnswer2(), resultBean.getResult());
                        } else if (!TextUtils.isEmpty(bean.getAnswer3()) && resultBean.getResult().contains(bean.getAnswer3())) {
                            CToast.getInstance().setDuration(CToast.CTOAST_LONG).showToast(bean.getAnswer3(), resultBean.getResult());
                        } else {
                            CToast.getInstance().setDuration(CToast.CTOAST_LONG).showToast("答案不确定", resultBean.getResult());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        CToast.getInstance().showToast("It`s error", "");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
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

            ocrSubject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseVirtualDisplay() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }
}
