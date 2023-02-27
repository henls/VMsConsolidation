from glob import glob
import os

path = r'/home/wxh/VMsConsolidation/modules/cloudsim-examples/src/main/resources/workload/planetlab/20110303-20'

files = glob(path + '/*')
for i in files:
    data = []
    with open(i, 'r') as f:
        cont = f.readlines()
    for j in cont:
        if int(j.strip()) > 100:
            j = '100\n'
        data.append(j)
    if os.path.exists(path + '-cap') == 0:
        os.mkdir(path + '-cap')
    with open(path + '-cap' + '/' + os.path.basename(i), 'w') as f:
        for k in data:
            f.writelines(k)
        