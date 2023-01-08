package org.cloudbus.cloudsim.FFTwxh2;

import java.util.Arrays;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFTcorrelate {
    

    public static double[] correlate(double[] a, double[] b){
        // Assume a, b is real, then a_img = 0, b_img = 0
        int length = a.length;
        double[] a_complex = new double[length * 2];
        double[] b_complex = new double[length * 2];

        DoubleFFT_1D fft = new DoubleFFT_1D(length);
        
        for (int i = 0; i < 2 * length; i += 2){
            a_complex[i] = a[i / 2];
            b_complex[i] = b[i / 2];
        }
        fft.complexInverse(a_complex, true);
        fft.complexForward(b_complex);
        double[] result_multiple = MultipleArray(a_complex, b_complex);
        fft.complexInverse(result_multiple, true);
        
        double[] abs_result = Abs(result_multiple);
        double max  = max(abs_result);
        int maxIndex = maxIndex(abs_result);
        double[] ret = new double[2];
        ret[0] = max;
        ret[1] = maxIndex;
        return ret;
    }

    public static double[] MultipleArray(double[] a, double[] b){
        int length = a.length;
        double[] result = new double[length];
        for (int i = 0; i < length; i+=2){
            result[i] = a[i] * b[i] - a[i + 1] * b[i + 1];
            result[i + 1] = a[i] * b[i + 1] + a[i + 1] * b[i];
        }
        return result;
    }

    public static double[] Abs(double[] a){
        double[] result = new double[a.length / 2];
        for (int i = 0; i < a.length; i += 2) {
            result[i / 2] = Math.sqrt(a[i] * a[i] + a[i + 1] * a[i + 1]);
        }
        return result;
    }

    public static double max(double[] arr){
        double max=arr[0]; 
        for(int i=0;i<arr.length;i++){
            if(arr[i]>max){
                max=arr[i];
            }
        }
        return max;
    }

    //获取最大值索引
    public static int maxIndex(double[] arr){
        int maxIndex=0;; 
        for(int i=0;i<arr.length;i++){
            if(arr[i]>arr[maxIndex]){
                maxIndex=i;
            }
        }
        return maxIndex;
    }
}
