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

	Map<String, Integer> upthreshold;

	public PowerVmAllocationPolicyMigrationLMSREGMUP(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
			double utilizationThreshold) {
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
		PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
		double upperThreshold = 0;

		

		addHistoryEntry(host, upperThreshold);
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedTotalMips();
		}
		double utilization = totalRequestedMips / host.getTotalMips();
		return utilization > upperThreshold;
	}

	protected double getUPT(){
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
		double mean_x = getMean(data[1:])
		for (int i = 0; i < hostNum - 1; i++){
			double x = utilization[i];
			double y = utilization[i + 1];

		}
	}

	protected double getMean(double[] data){
		double sum = 0;
		for (int i=0; i < data.length; i++){
			sum += data[i];
		}
		return sum / data.length;
	}
}
