import math
from transformer import get_batch, get_data
from glob import glob
import time
from sklearn.neural_network import MLPRegressor
import torch.nn as nn
import matplotlib.pyplot as plt
import torch

def plot_and_loss(eval_model, data_source, filename):
    total_loss = 0.
    test_result = torch.Tensor(0)
    truth = torch.Tensor(0)
    with torch.no_grad():
        for i in range(0, len(data_source) - 1):
            data, target = get_batch(data_source, i, 1)
            data = data.squeeze().reshape(1, -1)
            target = target.squeeze()[-1]
            target = torch.tensor([target])
            output = eval_model(data)
            output = torch.tensor(output)
            total_loss += criterion(output, target).item()
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
    plt.savefig('graph/{}.png'.format(filename.split('/')[-1]))
    plt.close()

    return total_loss / i

savePath = r'./pth/'
dirnames = glob(r'./VMs_usage/*')
criterion = nn.MSELoss()
for filename in dirnames:
    if 'adam_ee_ntu_edu_tw_uw_oneswarm' not in filename:
        continue
    train_data, val_data = get_data(filename)
    model = MLPRegressor(solver='lbfgs', alpha=1e-5,hidden_layer_sizes=(30,20), random_state=1)
    epochs = 100 # The number of epochs
    # if os.path.exists('graph/{}.png'.format(filename.split('/')[-1])):
    #     print(filename)
    #     continue
    epoch_start_time = time.time()

    train_data_x, train_target_y = get_batch(train_data, 0, 1484)
    train_data_x = train_data_x.squeeze().transpose(1, 0)
    train_target_y = train_target_y.squeeze().transpose(1, 0)[:, -1]
    model.fit(train_data_x, train_target_y)

    val_loss = plot_and_loss(model.predict, val_data, filename)
    print('-' * 89)
    print('| end of epoch {:3d} | time: {:5.2f}s | valid loss {:5.5f} | valid ppl {:8.2f}'.format(1, (
                time.time() - epoch_start_time), val_loss, math.exp(val_loss)))
    print('-' * 89)
    
