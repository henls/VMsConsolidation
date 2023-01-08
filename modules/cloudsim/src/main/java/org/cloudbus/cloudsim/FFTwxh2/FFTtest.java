package org.cloudbus.cloudsim.FFTwxh2;
public class FFTtest {
    public static void main(String[] args) {

        // These numbers are to run the FFTs and to check the accuracy of the optimized
        // versions, length = 32
        double[] inputReal32 = {1, 22, 33, 44, 15, 16, 17, 18, 1, 22, 33, 44, 15, 16, 17, 18, 1, 22, 33, 44, 15, 16, 17,
                18, 1, 22, 33, 44, 15, 16, 17, 18};
        double[] inputReal32_2 = {17, 18, 1, 22, 33, 44, 15, 16, 17, 18, 1, 22, 33, 44, 15, 16, 17, 18, 1, 22, 33, 44, 15, 16, 17,
            18, 1, 22, 33, 44, 15, 16};
        double[] result = FFTcorrelate.correlate(inputReal32, inputReal32_2);
        System.out.println(result[0] + " " + result[1]);
    }
}
