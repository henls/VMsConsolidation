package org.cloudbus.cloudsim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * Defines the resource utilization model based on 
 * a <a href="https://www.planet-lab.org">PlanetLab</a>
 * datacenter trace file.
 */
public class UtilizationModelPlanetLabInMemory implements UtilizationModel {
	
	/** The scheduling interval. */
	private double schedulingInterval;

	/** The data (5 min * 288 = 24 hours). */
	private final double[] data; 

	/** The CloudletName */
	private final String cloudletName;

	/** Workload data in previous days */
	private double[] previousData;

	/** Input path */
	private String filePath;

	/**
	 * Instantiates a new PlanetLab resource utilization model from a trace file.
	 * 
	 * @param inputPath The path of a PlanetLab datacenter trace.
         * @param schedulingInterval
	 * @throws NumberFormatException the number format exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ParameterException
	 */
	public UtilizationModelPlanetLabInMemory(String inputPath, double schedulingInterval)
			throws NumberFormatException,
			IOException, ParameterException {
		filePath = inputPath;
		data = new double[289];
		cloudletName = inputPath.split("/")[-1];
		setSchedulingInterval(schedulingInterval);
		BufferedReader input = new BufferedReader(new FileReader(inputPath));
		int n = data.length;
		for (int i = 0; i < n - 1; i++) {
			data[i] = Integer.parseInt(input.readLine()) / 100.0;
		}
		data[n - 1] = data[n - 2];
		input.close();
		// Load previous data
		previousData = LoadPreviousData(cloudletName);
	}
	
	/**
	 * Instantiates a new PlanetLab resource utilization model with variable data samples
         * from a trace file.
	 * 
	 * @param inputPath The path of a PlanetLab datacenter trace.
	 * @param dataSamples number of samples in the file
	 * @throws NumberFormatException the number format exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public UtilizationModelPlanetLabInMemory(String inputPath, double schedulingInterval, int dataSamples)
			throws NumberFormatException,
			IOException {
		setSchedulingInterval(schedulingInterval);
		data = new double[dataSamples];
		cloudletName = inputPath.split("/")[-1];
		BufferedReader input = new BufferedReader(new FileReader(inputPath));
		int n = data.length;
		for (int i = 0; i < n - 1; i++) {
			data[i] = Integer.parseInt(input.readLine()) / 100.0;
		}
		data[n - 1] = data[n - 2];
		input.close();
	}

	@Override
	public double getUtilization(double time) {
		if (time % getSchedulingInterval() == 0) {
			return data[(int) time / (int) getSchedulingInterval()];
		}
		int time1 = (int) Math.floor(time / getSchedulingInterval());
		int time2 = (int) Math.ceil(time / getSchedulingInterval());
		double utilization1 = data[time1];
		double utilization2 = data[time2];
		double delta = (utilization2 - utilization1) / ((time2 - time1) * getSchedulingInterval());
		double utilization = utilization1 + delta * (time - time1 * getSchedulingInterval());
		return utilization;

	}
	// wxh 这里调模型，做预测，从其他几天的数据中生成预训练模型并保存，这个模型负责读模型，微调，然后预测。
	@Override
	public double getUtilizationPredict(double time) {

		// model = ;
		// 预测
		// python transformerPredict.py model="CloudletName", data = data
		// if time % n == 0
		// update model (update cache = cached data)
		double currentUtil;
		if (time % getSchedulingInterval() == 0) {
			currentUtil = data[(int) time / (int) getSchedulingInterval()];
		}else{
			int time1 = (int) Math.floor(time / getSchedulingInterval());
			int time2 = (int) Math.ceil(time / getSchedulingInterval());
			double utilization1 = data[time1];
			double utilization2 = data[time2];
			double delta = (utilization2 - utilization1) / ((time2 - time1) * getSchedulingInterval());
			currentUtil = utilization1 + delta * (time - time1 * getSchedulingInterval());
		}

		String ans = "";
		try {
			String command = "python ./predictor/model/transformerAPI.py " + previousData + " " + Arrays.copyOfRange(data, (int) time / (int) getSchedulingInterval() - 10, (int) time / (int) getSchedulingInterval() - 1) + " " + currentUtil;
			Process process = Runtime.getRuntime().exec(command);
			ans = getResults(process);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Double.parseDouble(ans);
	}

	public double[] LoadPreviousData(String cloudletName) throws ParameterException, FileNotFoundException{
		double[] filenames = {20110303, 20110306, 20110309, 20110322, 20110325, 20110403, 20110409, 20110411, 20110412, 20110420};
		Path directPath = Paths.get(filePath);
		double day = Double.parseDouble(directPath.getParent().getFileName().toString());
		List<Path> pickFiles = new ArrayList<>();
		List<Double> dataList = new ArrayList<>();
		for (double item : filenames) {
			if (item < day){
				pickFiles.add(Paths.get(directPath.getParent().toString(), "" + item));
			}
		}
		for (Path path : pickFiles) {
			File folder = new File(path.toAbsolutePath().toString());
			String[] fileNameList = folder.list();
			String indata;
			for (String files : fileNameList) {
				if (files.contains(cloudletName)){
					BufferedReader input = new BufferedReader(new FileReader(files));
					try {
						indata = input.readLine();
						while(indata != null){
							dataList.add(Integer.parseInt(indata) / 100.0);
							indata = input.readLine();
						}
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		double[] dataArray = new double[dataList.size()];
		for (int i = 0; i < dataList.size(); i++) {
			dataArray[i] = dataList.get(i);
		}
		return dataArray;
	}


	public static String getResults(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        line = reader.readLine();
		return line;
    }

	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the new scheduling interval
	 */
	public void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}

	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the scheduling interval
	 */
	public double getSchedulingInterval() {
		return schedulingInterval;
	}
	
	public double[] getData(){
		return data;
	}
}
