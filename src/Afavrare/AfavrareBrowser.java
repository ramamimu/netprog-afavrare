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

                // ok, separate the body that accidentally got in header
                this.extractBody();
                System.out.printf("hdr:\n[%s]\n", this.header);

                // get status code
                String statusCode = this.getStatusCode();

                if(statusCode.equals("200")){
                    // ok read the remaining body
                    int contentLength = this.getContentLength();

                    if(contentLength==0){
                        // maybe dia chunk
                        System.out.printf("bd:\n[%s]\n", this.body);

                        // ok read berapa chunk need to be read
                        int chunkLen = this.getChunkLength();

                        while(chunkLen > 0){
                            this.read_body_response(bis, chunkLen);
                            chunkLen = this.read_chunk_len(bis);
                            if(chunkLen != 0){
                                chunkLen+=this.body.length();
                            }
                        }

                    }else{
                        this.read_body_response(bis, contentLength);
                    }



                    System.out.printf("\n-------\n%s\n", this.body);

                    this.getAllClickableLink();

                    // ask where user want to go
                    break;
                }else if(statusCode.charAt(0) == '3'){
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
        int prevlen = this.body.length();
        String response = "";
        int bytesRead = 0;
        while(remainingLength > 0){
            remainingLength = contentLength - this.body.length() - response.length();
            byte[] bufferInput = new byte[remainingLength];
            try{
                // try to read remaining byte
                bytesRead = bis.read(bufferInput);
                if(bytesRead < 0){
//                    System.out.printf("Ok ga ada yang bisa dibacalgi\n");
                    break;
                }else{
//                    System.out.printf("Happen\n");
                    // ok not -1, some byte readed, append to response
                    response += new String(bufferInput, 0, bytesRead);
//                    System.out.printf("bd len: %d, cnlen: %d, curresponselen %d, rmlen:%d\n",
//                            this.body.length(), contentLength, response.length(), remainingLength);
                }

            }
            catch (IOException ex){
                System.out.printf("Some exception happen, go in \n");
//                System.err.print(ex);
//                break;
            }

        }
        System.out.printf("Done\n");
        this.body +=response;
//        System.out.printf("Hasil:\n%s\n", this.body);

//        if(this.body.length() > contentLength){
//            System.out.printf("Melewati batas!\n");
//        }

    }

    private int read_chunk_len(BufferedInputStream bis){
        // cek if end with \r\n bodynya
//        if(this.body.endsWith("\r\n")){
//            System.out.printf("Iya cuyyy\n");
//        }
//        else{
//            System.out.printf("Kaga, ending:%s\n", this.body.substring(this.body.length()-20));
//        }

        boolean isOk=false;
        int chunkLen = 0;
        String response = "";
        int character;
        while(true){
            // read until \r\n
            try{
                if(bis.available()>0){
                    character = bis.read();
                    response += (char) character;
//                    System.out.printf("cur res: %s\n", response);
                    if(response.contains("\n")){
                        try{
                            int end = response.length()-2;
                            if(end < 0){
                                end = response.length();
                            }
                            chunkLen = Integer.parseInt(response.substring(0, end));
                            isOk=true;
                            break;
                        }
                        catch (NumberFormatException ex){
//                            System.out.printf("Res: %s\n", response);
                            if(response.length()==2){
                                response="";
                            }else{
                                break;
                            }
                        }
                    }
                }else{
                    break;
                }
            }
            catch (IOException ex){
                System.err.print(ex);
                break;
            }
        }
        if(isOk){
            System.out.printf("we got: %s\n", response);
            chunkLen = Integer.parseInt(response.substring(0, response.length()-2), 16);
            return chunkLen;
        }
        return 0;
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
        if(indexHTTP==-1){
            return "No header\n";
        }
        String statusCode = this.header.substring(indexHTTP+9, indexHTTP+12);
        System.out.printf("status code: %s\n", statusCode);
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
        dapet chunklength yang udah duluan kebaca
        Body nya:
        \n
        chunk-len
        isi
        chunk-len
        isi
        */
        System.out.printf("Getting chunk\n");
        String[] newline = this.body.split("\r\n");
        System.out.printf("Ok second line body is %s\n", newline[0]);
//        System.out.println(newline[1]);
//        System.out.printf("-----\n");
//        for(int i = 0;i<newline[1].length();i++){
//            System.out.printf("#%d: %c\n", i,newline[1].charAt(i));
//        }
        int num = Integer.parseInt(newline[0], 16);
        System.out.printf("In decimal: %d, also, lemme update body\n", num);

        int indexOfChunkLen = this.body.indexOf(newline[0]);
        this.body = this.body.substring(indexOfChunkLen+newline[0].length()+2);
        System.out.printf("bd:[%s]\n", this.body);
        return num;
    }



    private void extractBody(){
        int indexBodyStart = this.header.indexOf("\r\n\r\n");
        if(indexBodyStart != -1){
            this.body = this.header.substring(indexBodyStart+4);
//            System.out.printf("bd:[%s]\n", this.body);
            // keep newline in header
            this.header = this.header.substring(0, indexBodyStart+4).toLowerCase();
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
