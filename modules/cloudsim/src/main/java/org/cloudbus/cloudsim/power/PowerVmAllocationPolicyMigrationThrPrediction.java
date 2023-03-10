/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.FFTwxh2.FFTcorrelate;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.MathUtil;

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
public class PowerVmAllocationPolicyMigrationThrPrediction extends
	PowerVmAllocationPolicyMigrationInterQuartileRange{

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
	private double safetyParameter = 0;

	private HashMap<String, Double> hostUtilizationMap = new HashMap<String, Double>();

	/** The fallback VM allocation policy to be used when
         * the IQR over utilization host detection doesn't have
         * data to be computed. */
	private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

	/**
	 * Instantiates a new PowerVmAllocationPolicyMigrationInterQuartileRange.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param safetyParameter the safety parameter
	 * @param utilizationThreshold the utilization threshold
	 */
	public PowerVmAllocationPolicyMigrationThrPrediction(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
			double utilizationThreshold) {
		super(hostList, vmSelectionPolicy, 
				safetyParameter, fallbackVmAllocationPolicy, 
				utilizationThreshold);
	}

	/**
	 * Instantiates a new PowerVmAllocationPolicyMigrationInterQuartileRange.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param safetyParameter the safety parameter
	 */
	public PowerVmAllocationPolicyMigrationThrPrediction(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		super(hostList, vmSelectionPolicy, safetyParameter, fallbackVmAllocationPolicy);
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
		double previousUtil = _host.getPreviousUtilizationMips();
		double upperThreshold = 0;
		try {
			upperThreshold = 1 - getSafetyParameter() * getHostUtilizationIqr(_host);
		} catch (IllegalArgumentException e) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		addHistoryEntry(host, upperThreshold);
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getNextRequestedTotalMips();
		}
		double utilization = totalRequestedMips / host.getTotalMips();
		// double utilization = getHostUtilization(host, true);
		
		// System.out.println("previous Util : " + previousUtil / host.getTotalMips());
		return utilization > 1;
	}

	// @Override
	// protected PowerHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {
	// 	PowerHost underUtilizedHost = null;
	// 	double totalUtilization = 0;
	// 	double validHost = 0;

	// 	for (PowerHost host : this.<PowerHost> getHostList()) {
	// 		if (excludedHosts.contains(host)) {
	// 			continue;
	// 		}
	// 		validHost += 1;
	// 		double utilization = getHostUtilization(host, false);

	// 		// double utilization = getHostUtilization(host, false);

	// 		totalUtilization += utilization;
	// 	}

	// 	double avgUtilization = totalUtilization / validHost;

	// 	for (PowerHost host : this.<PowerHost> getHostList()) {
	// 		if (excludedHosts.contains(host)) {
	// 			continue;
	// 		}
	// 		double utilization = getHostUtilization(host, false);
			
	// 		// double utilization = getHostUtilization(host, true);

	// 		if (utilization > 0 && utilization < 0.8 - avgUtilization
	// 				&& !areAllVmsMigratingOutOrAnyVmMigratingIn(host)) {
	// 			underUtilizedHost = host;
	// 			return underUtilizedHost;
	// 		}
	// 	}
	// 	return underUtilizedHost;
	// }

	public double getHostUtilization(PowerHost host, boolean nxt){
		if (nxt == false){
			double totalRequestedMips = 0;
			for (Vm vm : host.getVmList()) {
				totalRequestedMips += vm.getCurrentRequestedTotalMips();
			}
			double utilization = totalRequestedMips / host.getTotalMips();
			return utilization;
		}
		double totalRequestedMips = 0;
		String keyString = "" + host + (int) CloudSim.clock();
		double utilization = 0;
		if (hostUtilizationMap.containsKey(keyString)==false){
			for (Vm vm : host.getVmList()) {
				totalRequestedMips += vm.getNextRequestedTotalMips();
			}
			utilization = totalRequestedMips / host.getTotalMips();
			hostUtilizationMap.put(keyString, utilization);//把时间加进来
		}else{
			utilization = hostUtilizationMap.get(keyString);
		}
		return utilization;
	}

	@Override
	public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
		double minPower = Double.MAX_VALUE;
		double minCorr = Double.MAX_VALUE;
		PowerHost allocatedHost = null;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			if (excludedHosts.contains(host)) {
				continue;
			}
			if (host.isSuitableForVm(vm)) {
				if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
					continue;
				}

				try {
					/*phase correlation */
					PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
					if (_host.getUtilizationHistory().length > 1){
					// if (false){
						PowerVm _vm = (PowerVm) vm;
						double[] vmUsage = new double[_vm.getUtilizationHistory().size()];
						int counter = 0;
						for (double value : _vm.getUtilizationHistory()) {
							vmUsage[counter] = value;
							counter += 1;
						}
						double corr = FFTcorrelate.correlate(_host.getUtilizationHistory(), vmUsage)[0];
						if (corr < minCorr) {
							minCorr = corr;
							allocatedHost = host;
						}
					}
					/*PABSFD */
					else{
						double powerAfterAllocation = getPowerAfterAllocation(host, vm);
						if (powerAfterAllocation != -1) {
							double powerDiff = powerAfterAllocation - host.getPower();
							if (powerDiff < minPower) {
								minPower = powerDiff;
								allocatedHost = host;
							}
						}
					}
					

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return allocatedHost;
	}

}
