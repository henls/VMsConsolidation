/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.MathUtil;
import org.cloudbus.cloudsim.FFTwxh2.*;

/**
 * A VM selection policy that selects for migration the VM with the Maximum Correlation Coefficient (MCC) among 
 * a list of migratable VMs.
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
public class PowerVmSelectionPolicyMaximumCorrelationFFT extends PowerVmSelectionPolicy {

	/** The fallback VM selection policy to be used when
         * the  Maximum Correlation policy doesn't have data to be computed. */
	private PowerVmSelectionPolicy fallbackPolicy;

	/**
	 * Instantiates a new PowerVmSelectionPolicyMaximumCorrelation.
	 * 
	 * @param fallbackPolicy the fallback policy
	 */
	public PowerVmSelectionPolicyMaximumCorrelationFFT(final PowerVmSelectionPolicy fallbackPolicy) {
		super();
		setFallbackPolicy(fallbackPolicy);
	}

	@Override
	public Vm getVmToMigrate(final PowerHost host) {
		List<PowerVm> migratableVms = getMigratableVms(host);
		if (migratableVms.isEmpty()) {
			return null;
		}
		int metrics = 0;
		try {
			metrics = getCorrelationCoefficientsFFT(getUtilizationMatrix(migratableVms));
		} catch (IllegalArgumentException e) { // the degrees of freedom must be greater than zero
			return getFallbackPolicy().getVmToMigrate(host);
		}
		return migratableVms.get(metrics);
	}

	/**
	 * Gets the CPU utilization percentage matrix for a given list of VMs.
	 * 
	 * @param vmList the VM list
	 * @return the CPU utilization percentage matrix, where each line i
         * is a VM and each column j is a CPU utilization percentage history for that VM.
	 */
	protected double[][] getUtilizationMatrix(final List<PowerVm> vmList) {
		int n = vmList.size();
                /*//TODO It gets the min size of the history among all VMs considering
                that different VMs can have different history sizes.
                However, the j loop is not using the m variable
                but the size of the vm list. If a VM list has 
                a size greater than m, it will thow an exception.
                It as to be included a test case for that.*/
		int m = getMinUtilizationHistorySize(vmList);
		double[][] utilization = new double[n][m];
		for (int i = 0; i < n; i++) {
			List<Double> vmUtilization = vmList.get(i).getUtilizationHistory();
			for (int j = 0; j < vmUtilization.size(); j++) {
				utilization[i][j] = vmUtilization.get(j);
			}
		}
		return utilization;
	}

	/**
	 * Gets the min CPU utilization percentage history size among a list of VMs.
	 * 
	 * @param vmList the VM list
	 * @return the min CPU utilization percentage history size of the VM list
	 */
	protected int getMinUtilizationHistorySize(final List<PowerVm> vmList) {
		int minSize = Integer.MAX_VALUE;
		for (PowerVm vm : vmList) {
			int size = vm.getUtilizationHistory().size();
			if (size < minSize) {
				minSize = size;
			}
		}
		return minSize;
	}

	/**
	 * Gets the correlation coefficients.
	 * 
	 * @param data the data
	 * @return the correlation coefficients
	 */
	protected List<Double> getCorrelationCoefficients(final double[][] data) {
		int n = data.length;
		int m = data[0].length;
		List<Double> correlationCoefficients = new LinkedList<>();
		for (int i = 0; i < n; i++) {
			double[][] x = new double[n - 1][m];
			int k = 0;
			for (int j = 0; j < n; j++) {
				if (j != i) {
					x[k++] = data[j];
				}
			}

			// Transpose the matrix so that it fits the linear model
			double[][] xT = new Array2DRowRealMatrix(x).transpose().getData();

			// RSquare is the "coefficient of determination"
			correlationCoefficients.add(MathUtil.createLinearRegression(xT,
					data[i]).calculateRSquared());
		}
		return correlationCoefficients;
	}

	protected int getCorrelationCoefficientsFFT(final double[][] data) {
		int n = data.length;
		double[][] x = new double[n][n];
		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++) {
				if (j != i) {
					x[i][j] = FFTcorrelate.correlate(stdDataList(data[i]), stdDataList(data[j]))[0];
				}else{
					x[i][j] = 0;
				}
			}
		}
		return getMigrateVmId(x);
	}

	public static double[] stdDataList(double[] inputList) {
		// 判断是否为空
		if (inputList == null || inputList.length == 0) {
			return null;
		}
		// 计算平均值、标准差
		double sum = 0.0;
		for (double item : inputList) {
			sum += item;
		}
		double mean = sum / inputList.length;
	
		double temp = 0.0;
		for (double item : inputList) {
			temp += (item - mean) * (item - mean);
		}
		double standardDeviation = Math.sqrt(temp / inputList.length);
	
		// 计算转换后的值
		double[] outputList = new double[inputList.length];
		for (int i = 0; i < inputList.length; i++){
			outputList[i] = (inputList[i] - mean) / standardDeviation;
		}
		
		return outputList;
	}

	// protected int getMigrateVmId(double[][] data){
	// 	/*迁移干扰最大的vm */
	// 	int[] maxId = {0, 0};
	// 	double max = 0;
	// 	int n = data.length;
	// 	for (int i = 0; i < n; i++) {
	// 		for (int j = 0; j < n; j++) {
	// 			if (max < data[i][j]){
	// 				maxId[0] = i;
	// 				maxId[1] = j;
	// 				max = data[i][j];
	// 			}
	// 		}
	// 	}
	// 	data[maxId[0]][maxId[1]] = 0;
	// 	data[maxId[1]][maxId[0]] = 0;
	// 	double max_a = 0;
	// 	for (int i = 0; i < n; i++){
	// 		if (max_a < data[maxId[0]][i]){
	// 			max_a = data[maxId[0]][i];
	// 		}
	// 	}
	// 	double max_b = 0;
	// 	for (int i = 0; i < n; i++){
	// 		if (max_b < data[maxId[1]][i]){
	// 			max_b = data[maxId[1]][i];
	// 		}
	// 	}
	// 	int ret = 0;
	// 	if (max_a > max_b){
	// 		ret = maxId[0];
	// 	}else{
	// 		ret = maxId[1];
	// 	}
	// 	return ret;
	// }

	protected int getMigrateVmId(double[][] data){
		/*第二种判断迁移VM的算法，累加相关值，选最大的那个 */
		int maxId = 0;
		double max = 0;
		int n = data.length;
		for (int i = 0; i < n; i++) {
			double Temp = 0.;
			for (int j = 0; j < n; j++) {
				Temp += data[i][j];
			}
			if (Temp > max){
				maxId = i;
			}
		}
		return maxId;
	}

	/**
	 * Gets the fallback policy.
	 * 
	 * @return the fallback policy
	 */
	public PowerVmSelectionPolicy getFallbackPolicy() {
		return fallbackPolicy;
	}

	/**
	 * Sets the fallback policy.
	 * 
	 * @param fallbackPolicy the new fallback policy
	 */
	public void setFallbackPolicy(final PowerVmSelectionPolicy fallbackPolicy) {
		this.fallbackPolicy = fallbackPolicy;
	}

}
