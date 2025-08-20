import sys
import time
import threading
sys.path.append('/home/yyq/mavlink')
from python_gh import MAVLink
from pymavlink import mavutil
import os

# 可选：设置自定义协议路径
# os.environ["MAVLINK20"] = "1"  # 使用 MAVLink 2.0
# os.environ["MAVLINK_DIALECT"] = "/home/yyq/mavlink/message_definitions/v1.0/gh.xml"  # 指向 custom.xml

def message_receiver(master):
    """Thread function to continuously receive and process MAVLink messages"""
    while True:
        msg = master.recv_match(blocking=True)
        if msg:
            print(f"\nReceived id: {msg.get_msgId()} message: {msg.get_type()}")
            #print(msg)

def main():
    # Create a MAVLink connection
    #master = mavutil.mavlink_connection('udp:0.0.0.0:14550', dialect='gh')
    master = mavutil.mavlink_connection('udp:0.0.0.0:14550')

    mav = MAVLink(master)
    # Start receiver thread
    receiver_thread = threading.Thread(
        target=message_receiver,
        args=(master,),
        daemon=True
    )
    receiver_thread.start()

        # 增加等待时间
    print("等待心跳包...")
    master.wait_heartbeat(timeout=15)
    print(f"成功连接到 MAVLink 系统 (system={master.target_system}, component={master.target_component})")

    while True:
        # Heartbeat message
        master.mav.heartbeat_send(
            type=mavutil.mavlink.MAV_TYPE_GCS,
            autopilot=mavutil.mavlink.MAV_AUTOPILOT_GENERIC,
            base_mode=0,
            custom_mode=0,
            system_status=mavutil.mavlink.MAV_STATE_ACTIVE
        )
        time.sleep(1)

        # Send a heartbeat message
        msg = mav.gh_message_encode(
            c=b'A',                # char
            s=b'Hello World',      # string
            u8=1,                  # uint8_t
            u16=1000,              # uint16_t
            u64=123456789,         # uint64_t
            s8=-1,                 # int8_t
            s16=-1000,             # int16_t
            s32=-123456,           # int32_t
            s64=-123456789,        # int64_t
            f=3.14                 # float
        )
        data = msg.pack(master.mav)
        master.write(data)

        time.sleep(1)

        # msg = mav.gh_cmd_ack_encode(
        #     command=1,
        #     result=1, 
        #     progress=100,   
        #     result_param2=10,
        #     target_system=1,    
        #     target_component=1
        # )
        msg = mav.gh_cmd_encode(
            target_system=56,
            target_component=0, 
            seq=1,   
            command_id=1,
            param1=1.0,    
            param2=1.0,
            param3=1.0,
            param4=1.0,
            param5=1,
            param6=1,
            param7=1
        )
        data = msg.pack(master.mav)
        master.write(data)





if __name__ == "__main__":
    main()
