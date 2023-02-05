# from sklearnex import patch_sklearn, unpatch_sklearn
# patch_sklearn()
import math
from transformer import get_batch, get_data
from glob import glob
import time
from sklearn.neural_network import MLPRegressor
from sklearn.linear_model import LinearRegression
import torch.nn as nn
import matplotlib.pyplot as plt
import torch
import numpy as np
import signal
import socket
from TorchMLP import MLPRegress
import joblib
# from scikitlearn_plus.neural_network import MLPRegressor_cuda

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

class MLP(object):

    def __init__(self):
        self.initSocket()
    
    def initSocket(self):
        self.serversocket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        # 获取本地主机名称
        # 设置一个端口
        port = 12345
        # 将套接字与本地主机和端口绑定
        self.serversocket.bind(('127.0.0.1',port))
        # 设置监听最大连接数
        self.serversocket.listen(5)
        # 获取本地服务器的连接信息
        self.serversocket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1) 
        myaddr = self.serversocket.getsockname()
        print("server adress:%s"%str(myaddr))

    def plot_and_loss(self, eval_model, data_source, filename):
        total_loss = 0.
        test_result = torch.Tensor(0)
        truth = torch.Tensor(0)
        with torch.no_grad():
            for i in range(0, len(data_source) - 1):
                data, target = get_batch(data_source, i, 1)
                data = data.squeeze().reshape(1, -1)
                target = target.squeeze()[-1]
                target = torch.tensor([target])
                try:
                    output = eval_model(data).to('cpu')
                except Exception as e:
                    output = eval_model(data)
                output = torch.tensor(output)
                total_loss += self.criterion(output, target).item()
                test_result = torch.cat((test_result, output[-1].view(-1).cpu()), 0)
                truth = torch.cat((truth, target))

        plt.plot(test_result, color="red", label='predict')
        plt.plot(truth, color="blue", label='truth')
        plt.xlabel('time')
        plt.ylabel('Usage(%)')
        plt.title('Loss: {}'.format(total_loss / i))
        plt.grid(True, which='both')
        plt.axhline(y=0, color='k')
        plt.legend()
        plt.savefig('predictor/graph/{}.png'.format(filename.split('/')[-1]))
        plt.close()

        return total_loss / i

    def testLaunch(self):
        dirnames = glob(r'predictor/VMs_usage/*')
        self.criterion = nn.MSELoss()
        for filename in dirnames:
            # if 'adam_ee_ntu_edu_tw_uw_oneswarm' not in filename:
            #     continue
            if 'planetlab6_goto_info_waseda_ac_jp_uw_oneswarm' not in filename:
                continue
            train_data, val_data = get_data(filename)
            model = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30, 20), random_state=1)
            # model = MLPRegressor_cuda(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1)
            # if os.path.exists('predictor/graph/{}.png'.format(filename.split('/')[-1])):
            #     print(filename)
            #     continue
            epoch_start_time = time.time()

            train_data_x, train_target_y = get_batch(train_data, 0, 1484)
            train_data_x = train_data_x.squeeze().transpose(1, 0)
            train_target_y = train_target_y.squeeze().transpose(1, 0)[:, -1]
            train_data_x = train_data_x.numpy()
            train_target_y = train_target_y.numpy()
            model.fit(train_data_x, train_target_y)

            val_loss = self.plot_and_loss(model.predict, val_data, filename)
            print('-' * 89)
            print('| end of epoch {:3d} | time: {:5.2f}s | valid loss {:5.5f} | valid ppl {:8.2f}'.format(1, (
                        time.time() - epoch_start_time), val_loss, math.exp(val_loss)))
            print('-' * 89)

    def testTorchMLP(self):
        dirnames = glob(r'predictor/VMs_usage/*')
        self.criterion = nn.MSELoss()
        for filename in dirnames:
            # if 'adam_ee_ntu_edu_tw_uw_oneswarm' not in filename:
            #     continue
            if 'planetlab6_goto_info_waseda_ac_jp_uw_oneswarm' not in filename:
                continue
            train_data, val_data = get_data(filename)
            # model = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1)
            model = MLPRegress(20, 30, 20, 1)

            # if os.path.exists('graph/{}.png'.format(filename.split('/')[-1])):
            #     print(filename)
            #     continue
            epoch_start_time = time.time()
            train_data_x, train_target_y = get_batch(train_data, 0, 1484)
            train_data_x = train_data_x.squeeze().transpose(1, 0)
            train_target_y = train_target_y.squeeze().transpose(1, 0)[:, -1]
            model.fit(train_data_x, train_target_y)

            val_loss = self.plot_and_loss(model.predict, val_data, filename)
            print('-' * 89)
            print('| end of epoch {:3d} | time: {:5.2f}s | valid loss {:5.5f} | valid ppl {:8.2f}'.format(1, (
                        time.time() - epoch_start_time), val_loss, math.exp(val_loss)))
            print('-' * 89)

    def getTrainData(self, data):
        quantile_windows = 20
        data = np.sort(np.lib.stride_tricks.sliding_window_view(data, (quantile_windows,)), axis=1)[:, int(quantile_windows * 0.6) - 1]
        train_data1, train_targets1, valid_x = self.split_data(data.reshape(1, -1), 20)
        return train_data1, train_targets1, valid_x

    def split_data(self, data, slide_length):
        train_data = np.lib.stride_tricks.sliding_window_view(data, (1, slide_length))
        train_data = np.vstack(np.squeeze(train_data))
        train_targets = np.hstack(data[:,slide_length:])
        return torch.from_numpy(train_data[:-1]), torch.from_numpy(train_targets), torch.from_numpy(data[:, -1 * slide_length:])

    def recv(self):
        self.clientsocket,addr = self.serversocket.accept()
        data = ''
        while 1:
            rec = self.clientsocket.recv(1024)
            data += rec.decode("utf-8")
            if 'END' in data:
                break
        return data

    def send(self, data):
        self.clientsocket.send(str(data).encode("utf-8"))

    def predict(self):
        print('Launch model')
        while 1:
            recvData = self.recv()
            [previousDataList, currentDataList, currentData, timeStamp, cloudletName, _] = recvData.split('$')
            AllData = self.str2array(previousDataList, currentDataList, currentData)
            train_data_x, train_target_y, valid_x = self.getTrainData(AllData)
            train_interval = 10 * 300
            if int(timeStamp) % train_interval == 0:
                model = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1)
                # model = LinearRegression()
                model.fit(train_data_x, train_target_y)
                joblib.dump(model, "/home/wangxinhua/{}.m".format(cloudletName))
            else:
                try:
                    model = joblib.load("/home/wangxinhua/{}.m".format(cloudletName))
                except FileNotFoundError:
                    model = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1)
                    # model = LinearRegression()
                    model.fit(train_data_x, train_target_y)
                    joblib.dump(model, "/home/wangxinhua/{}.m".format(cloudletName))
            # model = MLPRegress(20, 30, 20, 1)
            # if 'planetlab1_unineuchatel_ch_princeton_codeen' == cloudletName:
            #     print(valid_x)
            result = model.predict(valid_x)
            if result < [0]:
                result = [0.0]
            elif result > [1]:
                result = [1.0]
            self.send(result)

    def str2array(self, previousDataList, currentDataList, currentData):
        if len(previousDataList) <= 2:
            previousDataList = []
        else:
            previousDataList = [float(i) for i in previousDataList[1:-1].split(',')]
        currentDataList = [float(i) for i in currentDataList[1:-1].split(',')]
        currentData = [float(currentData)]
        return np.hstack([previousDataList, currentDataList, currentData])


if __name__ == '__main__':
    model = MLP()
    model.predict()
    # model.testLaunch()
    # model.testTorchMLP()
        
