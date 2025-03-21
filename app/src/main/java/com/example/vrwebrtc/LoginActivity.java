package com.example.vrwebrtc;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import com.example.arwebrtc.webrtc.NetworkUtils.*;
import com.example.arwebrtc.webrtc.NetworkUtils;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText roomIdInput;
    private EditText userIdInput;
    private Button createButton;
    private Button joinButton;
    private boolean pushStream = false;


    private String CREATE_MEETING = "https://vrclassserver.com/api/meeting/create";
    private String JOIN_MEETING = "https://vrclassserver.com/api/meeting/join";
    private String WS_SIGNAL_URL = "wss://vrclassserver.com/ws/meeting/";
    private String DOMAIN_IP = "192.168.31.88";
    private String CREATE_MEETING_TITLE = "create测试会议";
    private String JOIN_MEETING_TITLE = "join测试会议";
    private String JOIN_MEETING_STREAMID = "1902380239396159489";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getPermissions();
        initViews();
    }

    private void initViews() {
        roomIdInput = findViewById(R.id.roomIdInput);
        userIdInput = findViewById(R.id.userIdInput);
        createButton = findViewById(R.id.createButton);
        joinButton = findViewById(R.id.joinButton);

        String randomRoomId = "room_" + new SecureRandom().nextInt(1000);
        roomIdInput.setText(randomRoomId);
        String randomUserId = "user_" + new SecureRandom().nextInt(1000);
        userIdInput.setText(randomUserId);

        createButton.setOnClickListener(v -> {
            String roomId = roomIdInput.getText().toString();
            String userId = userIdInput.getText().toString();

            if (roomId.isEmpty() || userId.isEmpty()) {
                Toast.makeText(this, "请输入房间ID和用户ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建会议
            createMeeting(CREATE_MEETING_TITLE , userId, "用户" + userId);
        });

        joinButton.setOnClickListener(v -> {
            String roomId = roomIdInput.getText().toString();
            String userId = userIdInput.getText().toString();

            if (roomId.isEmpty() || userId.isEmpty()) {
                Toast.makeText(this, "请输入房间ID和用户ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建会议
            joinMeeting(JOIN_MEETING_TITLE, JOIN_MEETING_STREAMID , "用户" + userId);
        });
    }

    private void createMeeting(String title, String userId, String userName) {
        pushStream = true;

        // 创建请求体
        JSONObject userJson = new JSONObject();
        JSONObject requestJson = new JSONObject();
        try {
            userJson.put("id", userName);
            userJson.put("name", userName);
            requestJson.put("title", title);
            requestJson.put("user", userJson);
        } catch (JSONException e) {
            Toast.makeText(this, "创建请求失败", Toast.LENGTH_SHORT).show();
            return;
        }

        NetworkUtils.sendRequest(CREATE_MEETING,
                requestJson, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "创建会议请求失败: " + e.getMessage());
                            Toast.makeText(LoginActivity.this, "创建会议失败", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        handleMeetingResponse(response, userId);
                    }
                });
    }

    private void joinMeeting(String title, String userId, String userName) {
        pushStream = false;

        // 创建请求体
        JSONObject userJson = new JSONObject();
        JSONObject requestJson = new JSONObject();
        try {
            userJson.put("id", userName);
            userJson.put("name", userName);
            requestJson.put("id", userId);
            requestJson.put("user", userJson);
        } catch (JSONException e) {
            Toast.makeText(this, "创建请求失败", Toast.LENGTH_SHORT).show();
            return;
        }

        NetworkUtils.sendRequest(JOIN_MEETING ,
                requestJson, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "加入会议请求失败: " + e.getMessage());
                            Toast.makeText(LoginActivity.this, "加入会议失败", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        handleMeetingResponse(response, userId);
                    }
                });
    }

    private void handleMeetingResponse(Response response, String userId) throws IOException {
        try {
            String result = response.body().string();
            Log.d(TAG, "会议响应: " + result);

            Gson gson = new Gson();
            MeetingCreateResponse meetingResponse = gson.fromJson(result, MeetingCreateResponse.class);

            if (meetingResponse != null && meetingResponse.success && meetingResponse.data != null
                    && meetingResponse.data.meeting != null) {

                MeetingCreateResponse.Meeting meeting = meetingResponse.data.meeting;

                // 保存会议信息
                SharedPreferences prefs = getSharedPreferences("MeetingInfo", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("meetingId", meeting.id);
                editor.putString("title", meeting.title);
                editor.putString("mediaServer", meeting.mediaServer);
                editor.putString("mediaServerType", meeting.mediaServerType);
                editor.apply();

                // 在主线程中启动 WebRTCActivity
                runOnUiThread(() -> {
                    Intent intent = new Intent(LoginActivity.this, WebRTCActivity.class);
                    intent.putExtra("roomId", meeting.id);
                    intent.putExtra("userId", userId);
                    intent.putExtra("push", pushStream?1:0);
                    intent.putExtra("mediaServer", meeting.mediaServer);
                    intent.putExtra("mediaServerType", meeting.mediaServerType);
                    intent.putExtra("wsSignalUrl", WS_SIGNAL_URL);
                    intent.putExtra("domain_ip", DOMAIN_IP);
                    
                    startActivity(intent);
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "操作失败: " + meetingResponse.message,
                            Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                Log.e(TAG, "处理响应失败: " + e.getMessage());
                Toast.makeText(LoginActivity.this,
                        "处理响应失败", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void getPermissions() {
        String[] basicPermissions = {
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
                //android.Manifest.permission.ACCESS_NETWORK_STATE,
               // android.Manifest.permission.INTERNET,
                //android.Manifest.permission.ACCESS_WIFI_STATE
        };

        // 存储权限
        String[] storagePermissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 及以上使用 MANAGE_EXTERNAL_STORAGE
            storagePermissions = new String[]{
                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
            };
        } else {
            // Android 10 及以下使用传统存储权限
            storagePermissions = new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        // 先请求基本权限
        XXPermissions.with(this)
                .permission(basicPermissions)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            // 基本权限获取成功后，请求存储权限
                            requestStoragePermissions();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "请授予所有必要权限", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void requestStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 及以上，引导用户到设置页面开启所有文件访问权限
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            // Android 10 及以下，使用传统权限请求
            XXPermissions.with(this)
                    .permission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (!all) {
                                Toast.makeText(LoginActivity.this,
                                        "请授予存储权限", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}