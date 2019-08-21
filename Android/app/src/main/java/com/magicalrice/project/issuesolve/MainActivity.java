package com.magicalrice.project.issuesolve;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.magicalrice.project.issuesolve.utils.SPUtil;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

public class MainActivity extends AppCompatActivity implements MainView, View.OnClickListener {
    private static final int REQUEST_MEDIA_PROJECTION = 10002;

    private Button openAssist, clearCache;
    private ImageView imgScreenCapture;
    private boolean isShow = false;
    private MainPresenter presenter;
    private View view;
    private ImageView img_search, img_mic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        presenter = new MainPresenterImpl(this, MainActivity.this);
        imgScreenCapture = findViewById(R.id.screen_capture);
        openAssist = findViewById(R.id.btn_open);
        clearCache = findViewById(R.id.btn_clear);

        view = View.inflate(getApplicationContext(), R.layout.control_view, null);
        img_search = view.findViewById(R.id.btn_search);
        img_mic = view.findViewById(R.id.btn_mic);

        img_search.setOnClickListener(this);
        img_mic.setOnClickListener(this);
        openAssist.setOnClickListener(this);
        clearCache.setOnClickListener(this);

        isShow = (boolean) SPUtil.getInstance(this).get("isShowController", false);
        presenter.createEnvironment();
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

            presenter.resetCapture(resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        SPUtil.getInstance(this).put("isShowController", isShow);
        presenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void showSearchButton() {
        if (presenter.checkPersimission()) {
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
                openAssist.setText("关闭辅助窗口");
            } else {
                if (FloatWindow.get("subject") != null) {
                    FloatWindow.get("subject").hide();
                    FloatWindow.destroy("subject");
                    openAssist.setText("打开辅助窗口");
                }
            }
            isShow = !isShow;
        }
    }

    @Override
    public void showCapture(Bitmap bitmap) {
        if (bitmap != null) {
            imgScreenCapture.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                presenter.showSearchBtn();
                break;
            case R.id.btn_search:
                presenter.startCapture();
                break;
            case R.id.btn_mic:
                presenter.startMic();
                break;
            case R.id.btn_clear:
                presenter.clearCache();
                break;
        }
    }
}