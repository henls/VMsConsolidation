package org.cloudbus.cloudsim.power;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime;


public class API_java2python {

    public static void main(String[] args) throws Exception{
        String command = "python ./predictor/model/transformerAPI.py";
        Process process = Runtime.getRuntime().exec(command);
        printResults(process);
    }

    public static void printResults(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
