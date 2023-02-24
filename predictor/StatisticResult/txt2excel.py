import pandas as pd
from glob import glob
import re


pth = r'/home/wxh/VMsConsolidation/output/log/*.txt'
files = sorted(glob(pth))
dicts = {}
for idx, txt_file in enumerate(files):
    with open(txt_file, 'r') as f:
        data = f.read()
    data = data[data.index('Experiment name'):].strip().split('\n')
    for d in data:
        [k, v] = d.split(': ')
        if ',' in v:
            v = str([int(j.split('=')[-1])for j in v[1:-1].split(',')])
        dicts[k] = v
    if idx == 0:
        current= pd.DataFrame(dicts, index=[0])
    else:
        next = pd.DataFrame(dicts, index=[0])
        current = pd.merge(current, next, how = 'outer')
    print(current)
current.to_excel('/home/wxh/VMsConsolidation/predictor/result/consolidation_withAllocation_result.xlsx')
