package Afavrare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

public class AfavrareBrowser {
    // Attribute nya
    public Socket socket;
    public String header;
    public String body;
    public int statusCode;

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
            this.socket.setSoTimeout(3000); // timeout after 3 seconds

            // chaining socket's input stream to buffered input stream so that we are able to read more than 1 byte
            BufferedOutputStream bos = new BufferedOutputStream(this.socket.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());

            String http_request = this.make_http_request_msg("GET", "/", host);

            bos.write(http_request.getBytes(StandardCharsets.UTF_8), 0, http_request.length());
            bos.flush();

            // read the response
            this.header = this.read_header_response(bis);

            // ok, separate the body that accidentally got in header
            this.extractBody();

            // ok read the remaining body
            int contentLength = this.getContentLength();
            this.body += this.read_body_response(bis, contentLength);

            System.out.printf("%s\n", this.body);

        }catch (IOException ex){
            System.err.print(ex);
        }

    }

    private String make_http_request_msg(String method, String pathToResource, String host){
        String msg = "";
        msg += method + " " + pathToResource + " HTTP/1.1\r\n";
        msg += "Host: " + host + "\r\n\r\n";
        return msg;

    }

    private String read_header_response(BufferedInputStream bis){
        String response = "";
        byte[] bufferInput = new byte[512];
        int bytesRead = 0;
        boolean isReading=true;
        while(isReading){
            try{
                // try to read 512 byte
                bytesRead = bis.read(bufferInput);
                if(bytesRead < 0){
                    // -1 mean no byte to read
                    isReading=false;
                }else{
                    // ok not -1, some byte readed, append to response
                    response += new String(bufferInput, 0, bytesRead);
                    // stop if ada double new line (end of header)
                    isReading = isReading && !response.contains("\r\n\r\n");
                }

            }
            catch (IOException ex){
                break;
            }

        }

        return response;
    }

    private String read_body_response(BufferedInputStream bis, int contentLength){
        int remainingLength = contentLength - this.body.length();
        String response = "";
        byte[] bufferInput = new byte[512];
        int bytesRead = 0;
        while(response.length() < remainingLength){
            try{
                // try to read 512 byte
                bytesRead = bis.read(bufferInput);
                if(bytesRead < 0){
                    break;
                }else{
                    // ok not -1, some byte readed, append to response
                    response += new String(bufferInput, 0, bytesRead);
                }

            }
            catch (IOException ex){
                break;
            }

        }
        return response;
    }

    private int getContentLength(){

        int indexContentLength = this.header.indexOf("content-length:");
        int indexNewLine = this.header.indexOf("\r\n", indexContentLength);
        int contentLength=0;
        try{
            String integerPart = this.header.substring(indexContentLength+16, indexNewLine);
            contentLength = Integer.parseInt(integerPart);
        }
        catch (StringIndexOutOfBoundsException ex){
            //ignore
        }

        return contentLength;
    }



    private void extractBody(){
        int indexBodyStart = this.header.indexOf("\r\n\r\n");
        this.body = this.header.substring(indexBodyStart+2);//+2 to eliminate 2 newline

        // keep newline in header
        this.header = this.header.substring(0, indexBodyStart+2).toLowerCase();
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
