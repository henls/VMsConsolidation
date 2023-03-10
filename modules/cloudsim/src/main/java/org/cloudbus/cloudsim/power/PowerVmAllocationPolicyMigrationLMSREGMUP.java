/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.MathUtil;

import java.util.*;

/**
 * A VM allocation policy that uses Inter Quartile Range (IQR)  to compute
 * a dynamic threshold in order to detect host over utilization.
 * 
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 * 
 * <ul>
 * <li><a href="http://dx.doi.org/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley &amp; Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerVmAllocationPolicyMigrationLMSREGMUP extends
		PowerVmAllocationPolicyMigrationAbstract {

	/** The safety parameter in percentage (at scale from 0 to 1).
         * It is a tuning parameter used by the allocation policy to 
         * estimate host utilization (load). The host overload detection is based
         * on this estimation.
         * This parameter is used to tune the estimation
         * to up or down. If the parameter is set as 1.2, for instance, 
         * the estimated host utilization is increased in 20%, giving
         * the host a safety margin of 20% to grow its usage in order to try
         * avoiding SLA violations. As this parameter decreases, more
         * aggressive will be the consolidation (packing) of VMs inside a host,
         * what may lead to optimization of resource usage, but rising of SLA 
         * violations. Thus, the parameter has to be set in order to balance
         * such factors.
         */

	/**
	 * Instantiates a new PowerVmAllocationPolicyMigrationInterQuartileRange.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param safetyParameter the safety parameter
	 * @param utilizationThreshold the utilization threshold
	 */

	Map<Integer, Double> upthreshold = new HashMap<>();

	public PowerVmAllocationPolicyMigrationLMSREGMUP(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy) {
		super(hostList, vmSelectionPolicy);
	}

	/**
	 * Checks if the host is over utilized, based on CPU utilization.
	 * 
	 * @param host the host
	 * @return true, if the host is over utilized; false otherwise
	 */
	@Override
	protected boolean isHostOverUtilized(PowerHost host) {
		int time = (int) CloudSim.clock();
		double upperThreshold = 0;
		if (upthreshold.containsKey(time) == false){
			upperThreshold = getUPT();
			upthreshold.put(time, upperThreshold);
		}else{
			upperThreshold = upthreshold.get(time);
		}
		addHistoryEntry(host, upperThreshold);
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedTotalMips();
		}
		double utilization = totalRequestedMips / host.getTotalMips();
		return utilization > upperThreshold;
	}

	protected double getUPT(){
		double safetyParameter = 0.5;
		int hostNum = this.<PowerHost> getHostList().size();
		double[] utilization =  new double[hostNum];
		int counter = 0;
		for (PowerHost host : this.<PowerHost> getHostList()) {
			double totalRequestedMips = 0;
			for (Vm vm : host.getVmList()) {
				totalRequestedMips += vm.getCurrentRequestedTotalMips();
			}
			double util = totalRequestedMips / host.getTotalMips();
			utilization[counter] = util;
			counter += 1;
		}
		double alpha = getPara(utilization)[0];
		double beta = getPara(utilization)[1];

		double[] err = new double[hostNum - 2];
		
		for (int i = 0; i < hostNum - 2; i++){
			double _y = utilization[i] * beta + alpha;
			double y = utilization[i + 1];
			err[i] = (y - _y) * (y - _y);
		}
		return 1 - safetyParameter * getMad(err);
	}

	protected double getMean(double[] data){
		double sum = 0;
		for (int i=0; i < data.length; i++){
			sum += data[i];
		}
		return sum / data.length;
	}

	protected double getMad(double[] arr){
		for (int i = 0; i < arr.length - 1; i++) {
			//假设当前元素是最小的
			int minIndex = i;
			//从剩余的元素中找出最小的
			for (int j = i + 1; j < arr.length; j++) {
				if (arr[j] < arr[minIndex]) {
					//更新最小元素的索引
					minIndex = j;
				}
			}
			//如果最小元素不是当前元素，就交换它们
			if (minIndex != i) {
				double temp = arr[i];
				arr[i] = arr[minIndex];
				arr[minIndex] = temp;
			}
		}
		return arr[arr.length / 2];
	}

	protected double[] getPara(double[] data){
		double[] x = Arrays.copyOfRange(data, 0, data.length-2);
		double[] y = Arrays.copyOfRange(data, 1, data.length-1);
		double mean_x = getMean(x);
		double mean_y = getMean(y);
		double a = 0;
		double b = 0;
		for (int i = 0; i < data.length-2; i++){
			a += (x[i] - mean_x) * (y[i] - mean_y);
			b += (x[i] - mean_x) * (x[i] - mean_x);
		}
		double beta = a / b;
		double alpha = mean_y - beta * mean_x;
		double [] ret = {alpha, beta};
		return ret;
	}
}
