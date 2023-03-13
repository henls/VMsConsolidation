from glob import glob
a = glob(r'/home/wxh/VMsConsolidation/modules/cloudsim-examples/src/main/resources/workload/google/20110501-20-cap/*')
for i in a:
    with open(i, 'r') as f:
        cont = f.readlines()
    for j in cont:
        try:
            print(j.strip())
            if int(j.strip()) > 100:
                print(i)
        except Exception as e:
            print(e)
            pass