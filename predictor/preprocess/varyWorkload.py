from glob import glob
import os

def changeWorkload(pth, times):
    ret = []
    dirs = glob(pth + '/*')
    for file in dirs:
        with open(file, 'r') as f:
            fr = f.readlines()
        ret = [int(int(i) * times) for i in fr]
        saveName = os.path.dirname(pth) + '/' +  os.path.basename(pth)  + '-{}'.format(times)
        if os.path.exists(saveName) == False:
            os.mkdir(saveName)
        with open(saveName + '/' + os.path.basename(file), 'w+') as f:
            for i in ret:
                f.writelines(str(i) + '\n')
        print('save to {}'.format(saveName))


pth = r'/home/wxh/VMsConsolidation/modules/cloudsim-examples/src/main/resources/workload/planetlab/20110303'
times = 2.5

changeWorkload(pth, times)