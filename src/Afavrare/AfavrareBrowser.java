package Afavrare;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class AfavrareBrowser {
    // Attribute nya
    public Socket socket;
    public String header;
    public String body;
    public String host;
    public String currentLocation;
    public ArrayList<String> clickableLink;
    public ArrayList<String> hostsLink;
    public int waitCounter;
    public BufferedInputStream bis;
    public BufferedOutputStream bos;

    // Constructor nya
    public AfavrareBrowser(){
        this.clickableLink = new ArrayList<String>();
        this.waitCounter=0;
        this.hostsLink = new ArrayList<String>();
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
            this.socket.setSoTimeout(5000); // timeout after 3 seconds

            // chaining socket's input stream to buffered input stream so that we are able to read more than 1 byte
            this.bos = new BufferedOutputStream(this.socket.getOutputStream());
            this.bis = new BufferedInputStream(this.socket.getInputStream());

            this.make_send_http_request("GET", "/");
            this.currentLocation="/";
            while(true){
                // clear clickable link
                this.clickableLink.clear();
                this.hostsLink.clear();

                // read the response
                this.read_header_response();

                // ok, separate the body that accidentally got in header
                this.extractBody();
                System.out.printf("hdr:\n[%s]\n", this.header);

                // get status code
                String statusCode = this.getStatusCode();

                // get content type
                String contentType = this.getContentType();

                if(statusCode.equals("200")){
                    boolean continueLoop = handle_OK(contentType);
                    if(!continueLoop){
                        break;
                    }

                }else if(statusCode.charAt(0) == '3'){
                    // redirect
                    boolean didRedirect = this.handleRedirection();
                    if(! didRedirect){
                        break;
                    }

                }else{
                    if(statusCode.contains("No header")){
                        System.out.printf("No header, ok waiting again\n");
                        this.waitCounter +=1;
                        if(this.waitCounter > 5){
                            break;
                        }
                    }else{
                        System.out.printf("Status Code: %s\n", statusCode);
                        break;
                    }

                }



            }



        }catch (IOException ex){
            System.err.print(ex);
        }

    }

    private boolean askWhereToGo(){
        if(this.clickableLink.size() > 0){
            // ask where user want to go
            System.out.printf("Hello, Where you wanna go?? [Input a number (put negative or more than available link to stop]\n");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            int link = Integer.parseInt(input);
            if(link < 0 || link > this.clickableLink.size()){
                // stop dari user minta stop
                return false;
            }
            String dst = this.clickableLink.get(link);
            String host = this.hostsLink.get(link);
            System.out.printf("Going to:%s from %s eh?\n", dst, host);
            boolean success = this.changeConnection(host, dst);
            System.out.printf("Ok now host and loc: %s %s\n", this.host, this.currentLocation);
            if(!success){
                // stop, somehow gagal ganti link/host dll
                return false;
            }
            this.make_send_http_request("GET", dst);
            return true;
        }

        System.out.printf("No more link syre\n");
        // stop, no more link to click
        return false;
    }

    private boolean handle_OK(String contentType){
        System.out.printf("Ctype: %s\n", contentType);
        int contentLength = this.getContentLength();
        if(contentType.contains("text")){
            // ok read the remaining body

            if(contentLength==-1){
                // maybe dia chunk
                System.out.printf("bd:\n[%s]\n", this.body);

                // ok read berapa chunk need to be read
                int chunkLen = this.getChunkLength();

                // baca the rest of the body chunk-style
                while(chunkLen > 0){
                    this.read_body_response(chunkLen);
                    chunkLen = this.read_chunk_len();
                    if(chunkLen != 0){
                        chunkLen+=this.body.length();
                    }
                }

            }else{
                this.read_body_response(contentLength);
            }

            System.out.printf("\n-------\n%s\n", this.body);
            this.waitCounter=0;

            this.getAllClickableLink();
            return this.askWhereToGo();

        }
        else{

            // will save it as binary
            boolean success = this.downloadFile(contentLength);
            if(success){
                System.out.printf("Finish\n");
                return this.askWhereToGo();
            }
            return false;
        }
    }

    private boolean changeConnection(String host, String location){
        if(host.equals(this.host)){
            this.currentLocation = location;
            return true; // no need change
        }

        this.currentLocation = location;
        this.host=host;
        boolean success=false;
        try{

            this.socket = new Socket(this.host, 80);
            this.bos = new BufferedOutputStream(this.socket.getOutputStream());
            this.bis = new BufferedInputStream(this.socket.getInputStream());
            success=true;
            System.out.printf("Change host to %s and loc to %s\n", host, location);
        }
        catch (IOException ex){
            System.err.print(ex);
        }
        return success;
    }

    private String make_http_request_msg(String method, String pathToResource){
        String msg = "";
        msg += method + " " + pathToResource + " HTTP/1.1\r\n";
        msg += "Host: " + this.host + "\r\n";
//        msg += "Accept-encoding: chunked\r\n";
        msg += "\r\n\r\n";
        return msg;

    }

    private void make_send_http_request(String method, String pathToResource){
        String msg = this.make_http_request_msg(method, pathToResource);
        System.out.printf("msg:\n%s\n\n", msg);
        try{
            this.bos.write(msg.getBytes(StandardCharsets.UTF_8), 0, msg.length());
            this.bos.flush();
        }
        catch (IOException ex){
            System.err.print(ex);
        }
    }

    private boolean downloadFile(int contentLength){
        String filename = this.currentLocation.substring(1);// cutoff the /
        System.out.printf("file sir %s, with loc:%s\n", filename, this.currentLocation);

        try{
            FileOutputStream fileOut = new FileOutputStream(filename);
            // masukin body
            System.out.printf("dumping %d bytes\n", this.body.length(), this.body);
            fileOut.write(this.body.getBytes(), 0, this.body.length());
            int totalByteRead = this.body.length();
//            byte[] bufferInput = new byte[4096];
            int bytesRead;
            InputStream is = this.socket.getInputStream();
            byte[] bufferInput = new byte[1024];
            while(totalByteRead < contentLength){
                try{
                    bytesRead = is.read(bufferInput); // is faster somehow
                    if(bytesRead == -1){
                        break;
                    }
                    fileOut.write(bufferInput, 0, bytesRead);
                    totalByteRead+=bytesRead;
                    System.out.printf("Progress: %.2f as in %d/%d\n", (float) (totalByteRead*1.0)/contentLength, totalByteRead, contentLength);
//                    bytesRead=-5; // cek aja pas error return or no
                }
                catch (IOException ex){
                    System.err.print(ex);
//                    fileOut.close();
//                    return false;
                    System.out.printf("Trying again\n");
                }
            }
            // nyampe sini berarti telah berakhir
            fileOut.close();
            System.out.printf("Download done\n");
            return true;
        }
        catch (FileNotFoundException ex){
                System.err.print(ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void read_header_response(){
        String response = "";
        byte[] bufferInput = new byte[256];
        int bytesRead;
        boolean isReading=true;
        while(isReading){
            try{
                // try to read 256 byte
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

    private void read_body_response(int contentLength){
//        System.out.printf("ok %d %d\n", contentLength, this.body.length());
        int remainingLength = contentLength - this.body.length();
        int sizeByte;
        String response = "";
        int bytesRead;
        while(remainingLength > 0){
            remainingLength = contentLength - this.body.length() - response.length();
            sizeByte = Math.min(512, remainingLength);
            byte[] bufferInput = new byte[sizeByte];
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
//        System.out.printf("Done\n");
        this.body +=response;
//        System.out.printf("Hasil:\n%s\n", this.body);

//        if(this.body.length() > contentLength){
//            System.out.printf("Melewati batas!\n");
        if(this.body.endsWith("\r\n")){
//            System.out.printf("Ok cuk, ak makan rn\n");
            this.body =this.body.substring(0, this.body.length()-2);
        }
//        System.out.printf("I eat enough, %d, %d, %s\n", this.body.length(), contentLength, this.body.substring(this.body.length()-30));
    }

    private int read_chunk_len(){
        boolean isOk=false;
        int chunkLen;
        String response = "";
        StringBuilder nyempil = new StringBuilder();
        int character;

//        // cek if end with \r\n bodynya
//        if(!this.body.endsWith("\r\n")){
////            System.out.printf("Kaga, ending:%s\n", this.body.substring(this.body.length()-20));
//            // ok mundur cari a
        int totalMundur = 0;
        boolean sah = false;
        for(int i = this.body.length()-1;i>=0;i--){
            totalMundur+=1;
            if(totalMundur>50){
                break;
            }
            char curChar = this.body.charAt(i);
            if(curChar >= '0' && curChar <= '9'){
                nyempil.append(curChar);
            }
            else if(curChar == '\r'){
                sah=true;
                break;
            }else if(curChar != '\n'){
                break;
            }

        }
//
        System.out.printf("nyem: %s\n", nyempil.reverse());
        System.out.printf("last 50: %s\n", this.body.substring(this.body.length()-50));
//
//        }
        if(sah){
            response+=nyempil.reverse();
            System.out.printf("Initial  res now: %s\n", nyempil);
        }



        while(true){
            // read until \r\n
//            System.out.printf("Reading...\n");
            try{
                if(bis.available()>0){
                    character = bis.read();
                    response += (char) character;
//                    System.out.printf("cur res: %s, len:%d\n", response, response.length());
                    if(response.contains("\r\n")){
                        try{
                            int end = response.length()-2;
//                            System.out.printf("yow, %s with end:%d\n", response.substring(0, end), end);
                            // coba aja bisa gak di parsing
                            chunkLen = Integer.parseInt(response.substring(0, end), 16);
                            isOk=true;
                            break;
                        }
                        catch (NumberFormatException ex){
//                            System.out.printf("Res: %s, len:%d\n", response, response.length());
                            if(response.length()<3){
//                                System.out.printf("Reset\n");
                                response="";
                            }else{
//                                System.out.printf("No reset\n");
                                break;
                            }
                        }
                    }
                }else{
                    System.out.printf("HMM takde?\n");
                    break;
                }
            }
            catch (IOException ex){
                System.err.print(ex);
                break;
            }
        }
        if(isOk){
//            System.out.printf("we got: %s\n", response);
            chunkLen = Integer.parseInt(response.substring(0, response.length()-2), 16);
//            System.out.printf("chunklen: %d\n", chunkLen);
            return chunkLen;
        }
        return 0;
    }

    private boolean handleRedirection(){
        // cek for url=
        boolean doRedirect = false;

        int indexUrl = this.header.indexOf("location:");
        int indexNewLine = this.header.indexOf("\r\n", indexUrl);
        if(indexUrl != -1){
            System.out.printf("Redirection detected!\n");
            String redirectUrl = this.header.substring(indexUrl+10, indexNewLine);
            System.out.printf("%s\n", redirectUrl);

            // find the location relative from the host
            if(redirectUrl.contains("https")){
                System.out.printf("But it is https :(\n");
                return false;
            }

            String host = this.getHost(redirectUrl);
            String location = this.getLocation(redirectUrl);

            // check if we redirect to same loc
            if(location.equals(this.currentLocation) && host.equals(this.host)) {
                System.out.printf("Redirected to the same location, stop\n");
                return false;
            }


            boolean success = this.changeConnection(host, location);
            if(!success){
                System.out.printf("Fail to redirect\n");
                return false;
            }
            this.make_send_http_request("GET", location);
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
            indexHTTP = this.header.indexOf("http/1.0");
            if(indexHTTP==-1){
                return "No header\n";
            }
        }
//        String statusCode = this.header.substring(indexHTTP+9, indexHTTP+12);
//        System.out.printf("status code: %s\n", statusCode);
        return this.header.substring(indexHTTP+9, indexHTTP+12);
    }

    private String getContentType(){
        int indexContentType = this.header.indexOf("content-type:");
        int indexNewLine = this.header.indexOf("\r\n", indexContentType);
        if(indexContentType == -1){
            return "Nothing\n";
        }
//        String contentType = this.header.substring(indexContentType+13, indexNewLine);
        return this.header.substring(indexContentType+13, indexNewLine);
    }

    private int getContentLength(){

        int indexContentLength = this.header.indexOf("content-length:");
        int indexNewLine = this.header.indexOf("\r\n", indexContentLength);
        int contentLength=-1;
        try{
            String integerPart = this.header.substring(indexContentLength+16, indexNewLine);
            System.out.printf("ipart:[%s]\n", integerPart);
            if(integerPart.equals("")){
                return -1;
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
            System.out.printf("we got body: %d\n", this.body.length());
            // keep newline in header
            this.header = this.header.substring(0, indexBodyStart+4).toLowerCase();
        }else{
            this.body="";
        }
    }

    private String getHost(String link){
        int indexPisah = link.indexOf("://");
        if(indexPisah == -1){
            // ok maybe file biasa,
//            System.out.printf("no syre, this no host, so host is cur host\n");
            return this.host;
        }
        int indexSlash = link.indexOf("/", indexPisah+3);
        if(indexSlash==-1)indexSlash=link.length();
//        System.out.printf("ok host is: %s in %s\n", link.substring(indexPisah+3, indexSlash), link);
        String host = link.substring(indexPisah+3, indexSlash);
        if(host.endsWith("\"")){
            host  = host.substring(0, host.length()-1);
        }
        return host;
    }

    private String getLocation(String link){
        String host = getHost(link);

        // find the location relative from the host
        int indexHost = link.indexOf(host);
        if(indexHost == -1){
            // ok maybe ini file biasa
//            System.out.printf("ze host: %s, so the loc: %s\n", host, link);
            int idhref = link.indexOf("href=");
            if(idhref != -1){
                link = link.substring(idhref+5);

                // remove double quote if exist

                if(link.startsWith("\"")){
                    link = link.substring(1);
                }
                if(link.endsWith("\"")){
                    link = link.substring(0, link.length()-1);
                }

                // silang in awal
                if(!link.startsWith("/")){
                    link = "/" + link;
                }

            }

            return link;
        }
        int indexStartLoc = indexHost+host.length();
        String location = link.substring(indexStartLoc);
        if(location.endsWith("\"")){
            location = location.substring(0, location.length()-1);
        }
        if(location.equals("")){
            location="/";
        }
//        System.out.printf("Loc: %s\n", location);
        return location;
    }

    public void getAllClickableLink(){
        int idStart = 0;
        int idHref;
        int id = 0;
        while(idStart < this.body.length()){
            idHref = this.body.indexOf("href=", idStart);
            if(idHref==-1){
                break;
            }
            int idClose = this.body.indexOf(">", idHref+5);
            idStart=idClose;

            String hrefline = this.body.substring(idHref, idClose);

            int state = 0;
            int stopid = hrefline.length();
            char look = 0;
            for(int i = 5;i<hrefline.length();i++){
                if(state==0){
                    // looking for first "
                    if(hrefline.charAt(i)=='"'){
                        state=1;
                        look='"';
                    }else if(hrefline.charAt(i)=='\''){
                        state=1;
                        look='\'';
                    }
                }
                else if(state==1){
                    if(hrefline.charAt(i)==look){
                        stopid=i;
//                        System.out.printf("Ok stop at %d of %s\n", stopid, hrefline);
                        break;
                    }
                }
            }
            hrefline = hrefline.substring(0, stopid);
            if(hrefline.contains("https")){
                continue;
            }
            if(hrefline.contains("#")){
                continue;
            }
            if(hrefline.contains("void(0)")){
                continue;
            }


            System.out.printf("#%d: %s\n", id, hrefline);
            id+=1;
            this.clickableLink.add(this.getLocation(hrefline));
            this.hostsLink.add(this.getHost(hrefline));

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
