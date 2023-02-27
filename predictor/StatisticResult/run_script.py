import os
import sys
import multiprocessing

workloads = [20110306, 20110309, 20110322, 20110325, 
             20110403, 20110409, 20110411, 20110412, 20110420]

# VMselectionStrategy = ['mc', 'fftmc', 'mmt', 'mu', 'rs', 'peacr']
VMselectionStrategy = ['mc', 'fftmc', 'peacr']
# VMselectionStrategy = ['peacr']
# VMselectionStrategy = ['mc']
# OverloadedHostDetection = {'thr': 0.8, 'lr': 1.2, 'predThr': 1, 'iqr': 1.5, 'mad': 2.5, 'ACSVMC': 1}
OverloadedHostDetection = {'lr': 1.2, 'predThr': 1, 'iqr': 1.5, 'ACSVMC': 1}
# OverloadedHostDetection = {'lr': 1.2, 'predThr': 1, 'iqr': 1.5}
# OverloadedHostDetection = {'ACSVMC': 1}


fractile_quantile = [0.5, 0.6, 0.7, 0.8, 0.9, 1.0]

def QuantileImpact():
    for quantile in fractile_quantile:
        os.system("")

cmd = r'cd /home/wxh/VMsConsolidation ; /usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java @/tmp/cp_7o1v8fxh1ux6agafp4t5pxe8f.argfile org.cloudbus.cloudsim.examples.power.planetlab.run_script '

def DatasetImpact():
    for workload in workloads:
        os.system("{} {} {} {} {}".format(cmd, workload, 'predThr', 'mc', 1.0))

def functions(time, ohd, vmss, parameter, workload):
    os.system("{} {} {} {} {} {}".format(cmd, time, ohd, vmss, parameter, workload))

# times = ['20110303-cap', '20110303-20-cap', '20110303-15-cap', '20110501-cap', '20110501-15-cap', '20110501-20-cap']
times = ['20110501-cap', '20110501-15-cap', '20110501-20-cap']

def AlgorithmImpact():
    processes = []
    for vmss in VMselectionStrategy:
        for ohd, parameter in OverloadedHostDetection.items():
            # for time in ['20110303-25', '20110303-20', '20110303-15']:
            # for time in ['20110501-15', '20110501-20']:
            for time in times:
            # for time in ['20110501-cap', '20110501-15-cap', '20110501-20-cap']:
                if '20110501' in time:
                    workload = 'google'
                else:
                    workload = 'planetlab'
                if ohd != 'predThr':
                    _process = multiprocessing.Process(target = functions, args=(time, ohd, vmss, parameter, workload))
                    _process.start()
                    processes.append(_process)
    for _process in processes:
        _process.join() 
    for vmss in VMselectionStrategy:
        for ohd, parameter in OverloadedHostDetection.items():
            # for time in ['20110303-25', '20110303-20', '20110303-15']:
            for time in times:
            # for time in ['20110501-cap', '20110501-15-cap', '20110501-20-cap']:
                if '20110501' in time:
                    workload = 'google'
                else:
                    workload = 'planetlab'
                if ohd == 'predThr':
                    functions(time, ohd, vmss, parameter, workload)
AlgorithmImpact()

# DatasetImpact()
