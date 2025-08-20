package com.example.myapplication;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import com.MAVLink.MAVLinkPacket;
import com.example.MAVLINK.ByteArray;
import com.example.MAVLINK.gh.msg_gh_cmd;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MavlinkUdpHandler {
    private static final String TAG = "MavlinkUdpHandler";
    private static final int DRONE_PORT = 14566; // MAVROS 默认 UDP 端口

    private static final int INCOMPAT_FLAG_SIGNED = 0x01;
    private DatagramSocket socket;
    private InetAddress droneAddress;
    private Thread receiveThread;
    private boolean isRunning = false;

    public static MAVLinkPacket fromV2Bytes(byte[] rawBytes) {
        ByteArray bytes = new ByteArray(rawBytes);
        int versionMarker = bytes.getInt8(0);
        int payloadLength = bytes.getInt8(1);
        int incompatibleFlags = bytes.getInt8(2);
        int compatibleFlags = bytes.getInt8(3);
        int sequence = bytes.getInt8(4);
        int systemId = bytes.getInt8(5);
        int componentId = bytes.getInt8(6);
        int messageId = bytes.getInt24(7);
        byte[] payload = bytes.slice(10, payloadLength);
        int checksum = bytes.getInt16(10 + payloadLength);
        byte[] signature;
        if ((incompatibleFlags & INCOMPAT_FLAG_SIGNED) != 0) {
            signature = bytes.slice(12 + payloadLength, 13);
        } else {
            signature = new byte[0];
        }

        MAVLinkPacket mPack = new MAVLinkPacket(payloadLength);
        mPack.incompatFlags = incompatibleFlags;
        mPack.compatFlags = compatibleFlags;
        mPack.seq = sequence;
        mPack.sysid = systemId;
        mPack.compid = componentId;
        mPack.msgid = messageId;
        if (payload != null) {
            for (int i = 0; i < payload.length; i++) {
                mPack.payload.add(payload[i]);
            }
        }
        mPack.isMavlink2 = true;
        return mPack;
    }

    // 初始化并连接无人机
    public void connect(String droneIp) {
        try {
            droneAddress = InetAddress.getByName(droneIp);
            socket = new DatagramSocket(14550); // 绑定随机本地端口
            startReceiver();
            isRunning = true;
            Log.i(TAG, "Connected to " + droneIp + ":" + DRONE_PORT);
        } catch (Exception e) {
            Log.e(TAG, "Connection error", e);
        }
    }

    // 发送 MAVLink 消息
    public void sendMavlinkMessage(byte[] data) {
        if (socket == null || !isRunning) return;

        try {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, droneAddress, DRONE_PORT
            );
            socket.send(packet);
            Log.e(TAG, "Send success");
        } catch (IOException e) {
            Log.e(TAG, "Send error", e);
        }
    }

    // 启动接收线程
    private void startReceiver() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    processReceivedData(packet.getData(), packet.getLength());
                } catch (IOException e) {
                    if (isRunning) Log.e(TAG, "Receive error", e);
                }
            }
        });
        receiveThread.start();
    }

    // 处理接收到的 MAVLink 数据
    private void processReceivedData(byte[] data, int length) {
        // 解析 MAVLink 消息
        try {
            MAVLinkPacket packet = fromV2Bytes(data);
            com.MAVLink.Messages.MAVLinkMessage mm = packet.unpack();
            //Log.d(TAG, "Received msgid: "+packet.msgid+".");
            if(packet.msgid == 0)
            {
                Log.d(TAG, "Received heart beat " + mm.toString() + " ");
            }
        } catch (Exception e) {
            Log.e("MAVLink", "解析错误: " + e.getMessage());
        }
        //Log.d(TAG, "Received " + length + " bytes");
    }

    // 断开连接
    public void disconnect() {
        isRunning = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        Log.i(TAG, "Disconnected");
    }

}
