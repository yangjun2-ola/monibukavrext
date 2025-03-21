package com.example.vrwebrtc;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.arwebrtc.WebRTCManager;
import com.example.arwebrtc.webrtc.NetworkSpeedCallback;

import org.webrtc.SurfaceViewRenderer;

public class WebRTCActivity extends AppCompatActivity {
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private TextView networkSpeedText;
    private String roomId;
    private String userId;
    private String videoType;
    private WebRTCManager webRTCManager; // 来自AAR库
    private boolean isMuted = false;
    private boolean isFlashOn = false;
    private String mediaserverUrl;
    private String wsSignalUrl;
    private String domain_ip;
    private boolean pushStream = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc);

        // 获取传递的参数
        roomId = getIntent().getStringExtra("roomId");
        userId = getIntent().getStringExtra("userId");
        videoType =  getIntent().getStringExtra("mediaServerType");
        pushStream = getIntent().getIntExtra("push", 0) == 1;
        mediaserverUrl =  getIntent().getStringExtra("mediaServer");
        wsSignalUrl = getIntent().getStringExtra("wsSignalUrl");
        domain_ip = getIntent().getStringExtra("domain_ip");

        initViews();
        initWebRTC();
        startNetworkSpeedMonitor(true);
    }

    private void initViews() {
        localVideoView = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        networkSpeedText = findViewById(R.id.networkSpeedText);

        findViewById(R.id.switchCameraButton).setOnClickListener(v -> webRTCManager.switchCamera());
        findViewById(R.id.muteButton).setOnClickListener(v -> toggleMute());
        findViewById(R.id.flashButton).setOnClickListener(v -> toggleFlash());
        findViewById(R.id.zoomInButton).setOnClickListener(v -> webRTCManager.zoomIn());
        findViewById(R.id.zoomOutButton).setOnClickListener(v -> webRTCManager.zoomOut());
        findViewById(R.id.screenshotButton).setOnClickListener(v -> takeScreenshot());
        findViewById(R.id.exitButton).setOnClickListener(v -> exitRoom());
    }

    private void initWebRTC() {
        webRTCManager = new WebRTCManager.Builder()
                .context(this)
                .roomId(roomId)
                .userId(userId)
                .pushStream(pushStream)
                .serverUrl(mediaserverUrl,wsSignalUrl,domain_ip)
                .localVideoView(localVideoView)
                .remoteVideoView(remoteVideoView)
                .build();


        webRTCManager.initializeVideo();
    }

    private void startNetworkSpeedMonitor(boolean monitor) {
        // 实现网速监控
        if(monitor) {
            webRTCManager.setNetworkSpeedCallback(new NetworkSpeedCallback() {
                @Override
                public void onNetworkSpeedUpdate(long uploadBandwidth, long downloadBandwidth,
                                                 long estimatedUploadBandwidth, long estimatedDownloadBandwidth) {
                    // 格式化网速信息
                    @SuppressLint("DefaultLocale") String speedInfo = String.format("↑ %d kbps\n↓ %d kbps\n预估↑ %d kbps\n预估↓ %d kbps",
                            uploadBandwidth,
                            downloadBandwidth,
                            estimatedUploadBandwidth,
                            estimatedDownloadBandwidth);

                    // 更新UI
                    networkSpeedText.setText(speedInfo);
                }
            });
        }else{
            webRTCManager.setNetworkSpeedCallback(null);
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        webRTCManager.setMicrophoneMute(isMuted);
        // 更新按钮图标
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        webRTCManager.setFlashLight(isFlashOn);
        // 更新按钮图标
    }

    private void takeScreenshot() {
//        webRTCManager.takeScreenshot(new WebRTCManager.ScreenshotCallback() {
//            @Override
//            public void onScreenshotTaken(Bitmap bitmap) {
//                // 保存截图
//                //saveBitmapToGallery(bitmap);
//            }
//        });
    }

    private void exitRoom() {
        if (webRTCManager != null) {
            webRTCManager.release();
            webRTCManager = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webRTCManager != null) {
            webRTCManager.release();
            webRTCManager = null;
        }
    }
}