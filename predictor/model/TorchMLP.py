import torch
import torch.nn as nn
import torch.optim as optim

import numpy as np
import time

from sklearn.neural_network import MLPRegressor
from pytorchtools import EarlyStopping

from lbfgsnew import LBFGSNew

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
# device = 'cpu'
print(device)

class MLP(nn.Module):
    
    def __init__(self, sequence_dim, hidden_in, hidden_out, label_dim):
        super(MLP, self).__init__()
        self.network = torch.nn.Sequential(
            nn.Linear(sequence_dim, hidden_in),
            nn.ELU(),
            nn.Linear(hidden_in, hidden_out),
            nn.ELU(),
            nn.Linear(hidden_out, label_dim),
            nn.Sigmoid()
        )
        
    def forward(self, input):
        input = input.to(torch.float)
        input = input.to(device)
        return self.network(input)

class MLPRegress(object):

    def __init__(self, sequence_dim, hidden_in, hidden_out, label_dim):
        
        self.model = MLP(sequence_dim, hidden_in, hidden_out, label_dim).to(device)
    
    def fit(self, train_data, train_target):
        criterion = nn.MSELoss()
        epoch = 1000
        # optimizer = LBFGSNew(self.model.parameters(), line_search_fn=True, batch_mode=False, tolerance_grad=2e-4, tolerance_change=1e-10)
        optimizer = optim.LBFGS(self.model.parameters(), lr=1e-0, max_iter=1500, tolerance_grad=2e-4, tolerance_change=1e-10)
        # optimizer = optim.Adam(self.model.parameters(), lr=1e-2)
        # optimizer = optim.RMSprop(self.model.parameters())
        train_target = train_target.reshape(-1, 1).to(torch.float)
        train_target = train_target.to(device)

        early_stopping = EarlyStopping(patience=100, verbose=False)
        def closure():
            for i in range(epoch):
                if torch.is_grad_enabled():
                    optimizer.zero_grad()
                predict = self.model(train_data)
                loss = criterion(predict, train_target) #+ 1e-2 * sum([(p.abs()).sum() for p in self.model.parameters()]) / sum([len(p) for p in self.model.parameters()])
                if loss.requires_grad:
                    loss.backward()
                early_stopping(loss, self.model)
                # if early_stopping.early_stop:
                #     return torch.tensor([0])
                return loss
        optimizer.step(closure)
        # for i in range(epoch):
        #     optimizer.zero_grad()
        #     predict = self.model(train_data)
        #     loss = criterion(predict, train_target) + 3e-2 * sum([(p.abs()).sum() for p in self.model.parameters()]) / sum([len(p) for p in self.model.parameters()])
        #     loss.backward()
        #     optimizer.step()
        #     early_stopping(loss, self.model)
        #     if early_stopping.early_stop:
        #         print("Early stopping")
        #         break
    def predict(self, valid_data):
        # self.model.eval()
        with torch.no_grad():
            self.model.load_state_dict(torch.load('checkpoint.pt'))
            return [self.model(valid_data).item()]

if __name__ == '__main__':
    model1 = MLPRegress(20, 30, 20, 1)
    train_Data1 = np.random.randn(857 * 20).reshape(857, 20)
    train_targets1 = np.random.randn(857 * 1).reshape(857, 1)
    test_data = np.random.randn(286 * 20).reshape(286, 20)
    start = time.time()
    for i in range(100):
        model1.fit(train_Data1,train_targets1)
        test_predict1 = model1.predict(test_data)
    print((time.time() - start) / 100)
    model1 = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1, max_iter=200, epsilon=1e-5)
    start = time.time()
    for i in range(100):
        model1.fit(train_Data1,train_targets1)
        test_predict1 = model1.predict(test_data)
    print((time.time() - start) / 100)

