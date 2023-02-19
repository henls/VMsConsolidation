import os
import sys
import multiprocessing

workloads = [20110306, 20110309, 20110322, 20110325, 
             20110403, 20110409, 20110411, 20110412, 20110420]

VMselectionStrategy = ['mc', 'fftmc', 'mmt', 'mu', 'rs']
OverloadedHostDetection = {'thr': 0.8, 'lr': 1.2, 'predThr': 1, 'iqr': 1.5, 'mad': 2.5}
# OverloadedHostDetection = {'thr': 0.8}

fractile_quantile = [0.5, 0.6, 0.7, 0.8, 0.9, 1.0]

def QuantileImpact():
    for quantile in fractile_quantile:
        os.system("")

def DatasetImpact():
    for workload in workloads:
        os.system("cd /home/wxh/VMsConsolidation ; /usr/bin/env /root/.vscode-server/extensions/redhat.java-1.13.0-linux-x64/jre/17.0.5-linux-x86_64/bin/java @/tmp/cp_7o1v8fxh1ux6agafp4t5pxe8f.argfile org.cloudbus.cloudsim.examples.power.planetlab.run_script  {} {} {} {}".format(workload, 'predThr', 'mc', 1.0))

def functions(time, ohd, vmss, parameter):
    os.system("cd /home/wxh/VMsConsolidation ; /usr/bin/env /root/.vscode-server/extensions/redhat.java-1.13.0-linux-x64/jre/17.0.5-linux-x86_64/bin/java @/tmp/cp_7o1v8fxh1ux6agafp4t5pxe8f.argfile org.cloudbus.cloudsim.examples.power.planetlab.run_script  {} {} {} {}".format(time, ohd, vmss, parameter))

def AlgorithmImpact():
    processes = []
    for vmss in VMselectionStrategy:
        for ohd, parameter in OverloadedHostDetection.items():
            for time in ['20110303-25', '20110303-20', '20110303-15']:
                if ohd != 'predThr':
                    _process = multiprocessing.Process(target = functions, args=(time, ohd, vmss, parameter))
                    _process.start()
                    processes.append(_process)
    for _process in processes:
        _process.join()
    for vmss in VMselectionStrategy:
        for ohd, parameter in OverloadedHostDetection.items():
            for time in ['20110303-25', '20110303-20', '20110303-15']:
                if ohd == 'predThr':
                    functions(time, ohd, vmss, parameter)
AlgorithmImpact()

# DatasetImpact()
