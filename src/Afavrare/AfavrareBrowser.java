package Afavrare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AfavrareBrowser {
    // Attribute nya
    public Socket socket;
    public String header;
    public String body;
    public String host;
    public ArrayList<String> clickableLink;
    public int statusCode;

    // Constructor nya
    public AfavrareBrowser(){
        this.clickableLink = new ArrayList<String>();
    }


    // start the browsing
    public void browse(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Hello Good sir/ma'am, Welcome to Afavrare Text-Based Browser\n");
        System.out.print("What will you browse to day? :D\n");
        System.out.print("Input a URL/Hostname/IP Address here\n");

        this.host = scanner.nextLine();
        int port = 80; // default HTTP

        // connect time after got user input
        try{
            this.socket = new Socket(this.host, port);
            this.socket.setSoTimeout(3000); // timeout after 3 seconds

            // chaining socket's input stream to buffered input stream so that we are able to read more than 1 byte
            BufferedOutputStream bos = new BufferedOutputStream(this.socket.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());

            this.make_send_http_request(bos, "GET", "/");
            while(true){
                // read the response
                this.read_header_response(bis);
                System.out.printf("hdr:\n[%s]\n", this.header);

                // get status code
                String statusCode = this.getStatusCode();

                if(statusCode.equals("200")){
                    // ok, separate the body that accidentally got in header
                    this.extractBody();

                    // ok read the remaining body
                    int contentLength = this.getContentLength();

                    if(contentLength==0){
                        // maybe dia chunk
                        System.out.printf("bd:\n[%s]\n", this.body);

                        // ok read berapa chunk need to be read
                        int chunkLen = this.getChunkLength();
                        break;
                    }

                    this.read_body_response(bis, contentLength);

                    System.out.printf("%s\n", this.body);

                    this.getAllClickableLink();

                    // ask where user want to go
                    break;
                }else if(statusCode.charAt(0) == '3'){
                    // pecah header sama body
                    this.extractBody();

                    // redirect
                    boolean didRedirect = this.handleRedirection(bos);
                    if(! didRedirect){
                        break;
                    }

                }else{
                    System.out.printf("Status Code: %s\n", statusCode);
                    break;
                }



            }



        }catch (IOException ex){
            System.err.print(ex);
        }

    }

    private String make_http_request_msg(String method, String pathToResource){
        String msg = "";
        msg += method + " " + pathToResource + " HTTP/1.1\r\n";
        msg += "Host: " + this.host + "\r\n";
//        msg += "Accept-encoding: chunked\r\n";
        msg += "\r\n\r\n";
        return msg;

    }

    private void make_send_http_request(BufferedOutputStream bos, String method, String pathToResource){
        String msg = this.make_http_request_msg(method, pathToResource);
        System.out.printf("msg:\n%s\n\n", msg);
        try{
            bos.write(msg.getBytes(StandardCharsets.UTF_8), 0, msg.length());
            bos.flush();
        }
        catch (IOException ex){
            System.err.print(ex);
        }
    }

    private void read_header_response(BufferedInputStream bis){
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

        this.header=response;
    }

    private void read_body_response(BufferedInputStream bis, int contentLength){
//        System.out.printf("ok %d %d\n", contentLength, this.body.length());
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
        this.body +=response;
    }

    private boolean handleRedirection(BufferedOutputStream bos){
        // cek for url=
        boolean doRedirect = false;

        int indexUrl = this.header.indexOf("location:");
        int indexNewLine = this.header.indexOf("\r\n", indexUrl);
        if(indexUrl != -1){
            System.out.printf("Redirection detected!\n");
            String redirectUrl = this.header.substring(indexUrl+10, indexNewLine);
            System.out.printf("%s\n", redirectUrl);

            // find the location relative from the host
            int indexHost = redirectUrl.indexOf(this.host);
            String protocol = redirectUrl.substring(0, indexHost);
            if(protocol.contains("https")){
                System.out.printf("But it is https :(\n");
                return false;
            }else{
                System.out.printf("Protoc: %s\n", protocol);
            }

            int indexStartLoc = indexHost+this.host.length();
            String location = redirectUrl.substring(indexStartLoc);
            System.out.printf("Loc: %s\n", location);

            this.make_send_http_request(bos,"GET", location);
            doRedirect=true;
            this.body="";
            this.header="";
        }else {
            // ok ga ada redirection
            System.out.printf("No redirection detected\n");
            System.out.printf("header:\n%s\n", this.header);
        }
        return doRedirect;
    }

    private String getStatusCode(){
        int indexHTTP = this.header.indexOf("http/1.1");
        String statusCode = this.header.substring(indexHTTP+10, indexHTTP+13);
        System.out.printf("s code: %s\n", statusCode);
        return statusCode;
    }

    private int getContentLength(){

        int indexContentLength = this.header.indexOf("content-length:");
        int indexNewLine = this.header.indexOf("\r\n", indexContentLength);
        int contentLength=0;
        try{
            String integerPart = this.header.substring(indexContentLength+16, indexNewLine);
            System.out.printf("ipart:[%s]\n", integerPart);
            if(integerPart.equals("")){
                return 0;
            }
            contentLength = Integer.parseInt(integerPart);
        }
        catch (StringIndexOutOfBoundsException ex){
            //ignore
        }

        return contentLength;
    }

    private int getChunkLength(){
        /*
        Body nya:
        \n
        chunk-len
        isi
        chunk-len
        isi
        */
        String[] newline = this.body.split("\n");
        System.out.printf("Ok second line body is: %s\n", newline[1]);
        return 256;
    }

    private void extractBody(){
        int indexBodyStart = this.header.indexOf("\r\n\r\n");
        if(indexBodyStart != -1){
            this.body = this.header.substring(indexBodyStart+2);
            // keep newline in header
            this.header = this.header.substring(0, indexBodyStart+2).toLowerCase();
        }else{
            this.body="";
        }
    }

    public void getAllClickableLink(){
        Pattern pattern = Pattern.compile("href=\"[h/](.*?)\"");
        Matcher matcher = pattern.matcher(this.body.toLowerCase());
        System.out.printf("Here are all clickable link syre\n");
        int counter = 0;
        while (matcher.find()) {
            String link = matcher.group();
            this.clickableLink.add(link);
            System.out.printf("#%d: %s\n", counter, link);
            counter+=1;
        }
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
