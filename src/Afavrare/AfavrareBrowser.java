package Afavrare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
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

            // chaining socket's input stream to buffered input stream so that we are able to read more than 1 byte
            BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(this.socket.getOutputStream());
            String http_request = this.make_http_request_msg("GET", "/index.html");
            bos.write(http_request.getBytes(StandardCharsets.UTF_8), 0, http_request.length());
            bos.flush();

            String response = new String(bis.readAllBytes());
            System.out.print(response);

            // end
            this.close();
        }catch (IOException ex){
            System.err.print(ex);
        }

    }

    public String make_http_request_msg(String method, String pathToResource){
        String msg = "";
        msg += method + " " + pathToResource + " HTTP/1.1\n";
        msg += "Host: " + this.socket.getInetAddress() + "\n";
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
