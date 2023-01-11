package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;

public class socketTest {

    public static void main(String[] args) {
            // try {
            //     while (true){
            //         Socket socket = new Socket("127.0.0.1",12345);
            //         PrintStream out = new PrintStream(socket.getOutputStream());
            //         out.print("java123");
            //         BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
            //         char[] rDataLen = new char[100];
            //         br.read(rDataLen, 0, 100);
            //         System.out.println(rDataLen);
            //     }
            // } catch (IOException e) {
            //         e.printStackTrace();
            // }
            DecimalFormat decimalFormat = new DecimalFormat("0.000");
            double data=1.0000000001827465E-9;
            System.out.println(decimalFormat.format(data));
            
    }
}
