package com.example.myapplication;
import static com.MAVLink.enums.MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
import static com.MAVLink.enums.MAV_STATE.MAV_STATE_ACTIVE;
import static com.MAVLink.enums.MAV_TYPE.MAV_TYPE_GCS;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import com.MAVLink.minimal.msg_heartbeat;
import com.example.MAVLINK.gh.msg_gh_cmd;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends AppCompatActivity {
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);
    private static final String TAG = "MAVLink";

    MavlinkUdpHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_main);

        try {
            handler = new MavlinkUdpHandler();
            handler.connect("192.168.1.162"); // 无人机 IP

            Timer heartbeatTimer = new Timer();
            heartbeatTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    com.MAVLink.MAVLinkPacket packet = createHeartbeat();
                    handler.sendMavlinkMessage(packet.encodePacket());
                    Log.d(TAG, "send heart beat.");
                }
            }, 0, 1000); // 立即开始，间隔 1000ms

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Button myButton = findViewById(R.id.button1);
        Log.d(TAG, "sendMavlinkMessage");
        myButton.setOnClickListener(v -> {
            // 点击事件处理逻辑
            Log.d(TAG, "Lambda 方式处理点击事件");
            EventBus.getDefault().post(new MessageEvent("Hello EventBus!"));
        });

    }

    public com.MAVLink.MAVLinkPacket createHeartbeat() {
        msg_heartbeat heartbeat = new msg_heartbeat();
        heartbeat.type = MAV_TYPE_GCS; // 地面站类型
        heartbeat.autopilot = MAV_AUTOPILOT_INVALID;
        heartbeat.base_mode = 0;
        heartbeat.custom_mode = 0;
        heartbeat.system_status = MAV_STATE_ACTIVE;
        Log.d(TAG, "createHeartbeat "+ heartbeat.toString());
        // 序列化消息
        return heartbeat.pack();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(MessageEvent event) {
        // 更新 UI
        msg_gh_cmd cmd = new msg_gh_cmd(1.0f,1.0f,1.0f,1.0f,1,1,1,2,2,(short) 56,(short)0);
        cmd.isMavlink2 = true;
        com.MAVLink.MAVLinkPacket cmd_packet = cmd.pack();
        handler.sendMavlinkMessage(cmd_packet.encodePacket());
        Log.i(TAG, "send msg_gh_cmd to fcu."+ cmd.toString());
    }
}