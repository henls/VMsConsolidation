import torch
import torch.nn as nn
import torch.optim as optim

import numpy as np
import time

from sklearn.neural_network import MLPRegressor

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
# device = 'cpu'
print(device)

class MLP(nn.Module):
    
    def __init__(self, sequence_dim, hidden_in, hidden_out, label_dim):
        super(MLP, self).__init__()
        self.network = torch.nn.Sequential(
            nn.Linear(sequence_dim, hidden_in),
            nn.Dropout(0.5),
            nn.Tanh(),
            nn.Linear(hidden_in, hidden_out),
            nn.Dropout(0.5),
            nn.Tanh(),
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
        # optimizer = optim.LBFGS(self.model.parameters(), max_iter=1500, tolerance_grad=1e-5)
        optimizer = optim.LBFGS(self.model.parameters())
        train_target = train_target.reshape(-1, 1).to(torch.float)
        train_target = train_target.to(device)
        def closure():
            optimizer.zero_grad()
            predict = self.model(train_data)
            loss = criterion(predict, train_target) + 5e-3 * sum([(p**2).sum() for p in self.model.parameters()]) / sum([len(p) for p in self.model.parameters()])
            loss.backward()
            return loss
        optimizer.step(closure)
    def predict(self, valid_data):
        self.model.eval()
        with torch.no_grad():
            return self.model(valid_data)

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

