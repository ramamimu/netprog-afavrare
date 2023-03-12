package Afavrare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AfavrareBrowser {
    // Attribute nya
    public Socket socket;

    // Constructor nya
    public AfavrareBrowser(){
    }


    // start the browsing
    public void browse(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Hello Good sir/ma'am, Welcome to Afavrare Text-Based Browser\n");
        System.out.print("What will you browse to day? :D\n");
        System.out.print("Input a URL/Hostname/IP Address here\n");

        String host = scanner.nextLine();
        int port = 80; // default HTTP


        // connect time after got user input
        try{
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(5000); // timeout after 5 seconds

            // chaining socket's input stream to buffered input stream so that we are able to read more than 1 byte
            BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(this.socket.getOutputStream());

            String http_request = this.make_http_request_msg("GET", "/index.html", host);
            System.out.print(http_request);

            bos.write(http_request.getBytes(StandardCharsets.UTF_8), 0, http_request.length());
            bos.flush();
            System.out.print("Done flushing\n");

            String response = "";
            int counter = 0;
            boolean isDone = false;
            int byteRead = 0;
            while(true) {
                byte[] input = new byte[512];
                try{
                    byteRead = bis.read(input);
                }
                catch (SocketTimeoutException ex){
                    break;
                }
//                System.out.print("Done reading\n");
                response += new String(input);

                String bufarray[] = response.split("\r\n");
//                System.out.printf("Done splitting, got over %d string after split\n", bufarray.length);

                for(int i = 0;i<bufarray.length;i++){
//                    System.out.printf("We got split #%d: %s\n", i+1,bufarray[i]);
                }

                for (int i =0; i<bufarray.length-1;i++) {
                    if(counter == 0) {
                        System.out.println(bufarray[i]);
                    }
                    else if (bufarray[i].length()>0) {
                        String[] header = bufarray[i].split(":");
                        System.out.println("=======\n" + header[0] + "\n" + header[1].trim());

                    }

                    else {
                        isDone = true;
                        break;
                    }

                    response = bufarray[bufarray.length-1];
                    if (isDone) {
                        break;
                    }

                    counter++;
                }
                System.out.print("For loop parsing is done\n");
            }
            // end
            this.close();

        }catch (IOException ex){
            System.err.print(ex);
        }

    }

    public String make_http_request_msg(String method, String pathToResource, String host){
        String msg = "";
        msg += method + " " + pathToResource + " HTTP/1.1\r\n";
//        msg += "Host: " + this.socket.getInetAddress() + "\n";
        msg += "Host: " + host + "\r\n";
        msg += "Connection: keep-alive\r\n";
        msg += "Accept-Language: en-US\r\n";
        msg += "Accept-Encoding: gzip, deflate\r\n";
        msg += "Accept: text/html\r\n\r\n";
        return msg;

    }

    // to close the browser and all the connection
    public void close(){
        if(this.socket != null){
            try{
                this.socket.close();
            }
            catch (IOException ex){
                // ignore (sama kaya di buku)
            }
        }
    }

}
