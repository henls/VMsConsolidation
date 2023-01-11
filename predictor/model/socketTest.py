#encoding=UTF-8
import socket
import sys
import time
import signal

def my_handler(signal, frame):
    global stop
    stop = True
    print('终止')

signal.signal(signal.SIGINT, my_handler) 
stop = False

def main():
    # 创建服务器套接字
    serversocket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    # 获取本地主机名称
    host = socket.gethostname()
    # 设置一个端口
    port = 12345
    # 将套接字与本地主机和端口绑定
    serversocket.bind((host,port))
    # 设置监听最大连接数
    serversocket.listen(5)
    # 获取本地服务器的连接信息
    serversocket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1) 
    myaddr = serversocket.getsockname()
    print("server adress:%s"%str(myaddr))
    # 循环等待接受客户端信息
    data = 0
    while True:
        # 获取一个客户端连接
        clientsocket,addr = serversocket.accept()
        print("socket adress:%s" % str(addr))
        rec = clientsocket.recv(1024)
        data += 1
        print('recv: {}'.format(rec.decode("utf-8")))
        clientsocket.send(str(data).encode("utf-8"))
        print('send: {}'.format(data))
        time.sleep(0.005)
        if stop:
            clientsocket.close()
            break
    
 
if __name__ == "__main__":
    main()