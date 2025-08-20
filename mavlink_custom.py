import socket
from pymavlink import mavutil
import sys
import time
import threading
sys.path.append('/home/yyq/mavlink')
from python_gh import MAVLink
# 设置 UDP 套接字
UDP_IP = "0.0.0.0"  # 监听所有接口
UDP_PORT = 14550     # 使用的端口
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

print(f"Listening for MAVLink messages on {UDP_IP}:{UDP_PORT}")
# Create MAVLink connection first
# Create a function for periodic sending
def send_periodic(sock, addr):
    mav = MAVLink(None)
    while True:
        try:
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
            data1 = msg.pack(mav)
            sock.sendto(data1, addr)
            time.sleep(1)
        except Exception as e:
            print(f"Error in sending thread: {e}")

# Start sending thread after first message received
first_msg_received = False
sender_thread = None

print(f"Listening for MAVLink messages on {UDP_IP}:{UDP_PORT}")
mav = MAVLink(None)

while True:
    # 接收 UDP 数据包
    data, addr = sock.recvfrom(2048)  # 2048 是接收缓冲区的大小，根据需要调整
    if not first_msg_received:
        first_msg_received = True
        sender_thread = threading.Thread(target=send_periodic, args=(sock, addr), daemon=True)
        sender_thread.start()
    # 解析 MAVLink 数据包
    try:
        # Manual MAVLink v2 parsing
        if len(data) >= 10:  # Minimum MAVLink v2 header size
            # Parse header (magic, payload length, incompat flags, compat flags, seq, sysid, compid, msgid)
            magic, plen, incompat_flags, compat_flags, seq, sysid, compid = data[0:7]
            msgid = (data[9] << 16) | (data[8] << 8) | data[7]  # Extended message ID (3 bytes)
            
            print(f"MAVLink v2 Header:")
            print(f"  Magic: 0x{magic:02x}")
            print(f"  Payload length: {plen}")
            print(f"  Incompat flags: 0x{incompat_flags:02x}")
            print(f"  Compat flags: 0x{compat_flags:02x}")
            print(f"  Sequence: {seq}")
            print(f"  System ID: {sysid}")
            print(f"  Component ID: {compid}")
            print(f"  Message ID: {msgid}")

            # Parse payload (if present)
            # if plen > 0 and len(data) >= 10 + plen:
            #     payload = data[10:10+plen]
            #     print(f"  Payload: {payload.hex()}")

            # Parse checksum (if present)
            # if len(data) >= 10 + plen + 2:
            #     checksum = data[10+plen:10+plen+2]
            #     print(f"  Checksum: 0x{int.from_bytes(checksum, 'little'):04x}")

    except Exception as e:
        print(f"Error parsing message: {e}")
