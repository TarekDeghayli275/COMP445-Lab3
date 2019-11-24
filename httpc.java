import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;

//from UCPClient
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
//

public class httpc{
    
    private static boolean isVerbose = false;
    private static boolean isGetRequest = false;
    private static boolean isPostRequest = false;
    private static boolean needHelp = false;
    private static boolean hasHeaderData = false;
    private static boolean hasInLineData = false;
    private static boolean readFromFile = false;
    private static String headerData = "";
    private static String inLineData = "";
    private static String filePath = "";
    private static String url = "";
    private static String hostName = "";
    private static String outsideDirectory = "";
    private static String arguments = "";
    private static String messagBuilder = "";
    private static String defaultPort = "8007";
    private static String[] protocol_host_args = new String[2];

    private static boolean AKW = false;

    /**
     * Starting point of the application.
     * @param args cmd arguments.
     */
    public static void main (String[] args){
        //create an object of transport() and initalize channel
        transport obj = new transport("client");
        //System.out.println("object created");
        obj.handShake();
        if ( args.length == 0){
            System.out.println("\nEnter httpc help to get more information.\n");
        }else{
            cmdParser(args);
        }
        if (needHelp) {
                help();
        }else if(isGetRequest){
                get(url);
        }else if (isPostRequest){
                post(url);
        }
        //System.out.println("client payload created and sending \n"+messagBuilder);
        //call obj.sendData() with messageBuilder
        obj.sendData("client", 8007, messagBuilder);
        //System.out.println("client done sending");
        //System.out.println("client now listening");
        String newPayload = obj.listen(); //to wait for response from the server
        //System.out.println("done");//calls another method that prints the payload
        //call terminatingHandShake()
        if(!isVerbose){
            String[] lines = newPayload.split(System.getProperty("line.separator"));
            int count=0;
            for(int i=0;i<lines.length;i++){
                if(lines[i].length()==1){
                    count = i+1;
                    break;
                }
            }
            for(int i=count;i<lines.length;i++){
                System.out.println(lines[i]);
            }
            isVerbose=false;
        }else{
            System.out.print(newPayload);
        }
    }

    /**
     * This method takes the cmd args and parses them according to the different conditions of the application.
     * @param args an array of the command line arguments.
     */
    public static void cmdParser(String[] args){
        for (int i =0; i<args.length; i++){
            if (args[i].equalsIgnoreCase("-v")){
                isVerbose = true;
            }else if (args[i].equalsIgnoreCase("-h")){
                hasHeaderData = true;
                headerData = headerData.concat(args[i+1]+"\r\n");
                i++;
            }else if (args[i].equalsIgnoreCase("-d")){
                hasInLineData = true;
                inLineData = (args[i+1]);
                i++;
            }else if (args[i].equalsIgnoreCase("-f")){
                readFromFile = true;
                filePath = (args[i+1]);
                i++;
            }else if (args[i].equalsIgnoreCase("get")){
                isGetRequest = true;
            }else if (args[i].equalsIgnoreCase("post")){
                isPostRequest = true;
            }else if (args[i].equalsIgnoreCase("help")){
                needHelp = true;
            }else{
                url = (args[i]);
            }
       }
    }

    /**
     * this methods parses the url into host and arguments
     * @param url is the url to which the get/post request is made. example - 'httpbin.org/post'
     */
    public static void urlParser(String url) {
        if(url.contains("../")){
            protocol_host_args = url.split("/", 2);
            outsideDirectory = protocol_host_args[0];
            arguments = protocol_host_args[1];
        }else if (url.contains("//")){
            protocol_host_args = url.split("//");
            if (url.contains("/")){
                protocol_host_args = protocol_host_args[1].split("/");
                hostName = protocol_host_args[0];
                arguments = protocol_host_args[1];
            }
        }else if (url.contains("/")){
            protocol_host_args = url.split("/", 2);
            hostName = protocol_host_args[0];
            arguments = protocol_host_args[1];
        }
        
        else{
            hostName=url;
        }

        if (hostName.contains("localhost")){
            protocol_host_args = hostName.split(":", 2);
            hostName = protocol_host_args[0];
            defaultPort = protocol_host_args[1];
        }
    }

    /**
     * This method takes the data provided after the -d option and parses it.
     * @param inLineData is the data from the cmd after -d
     * @return a string that contains the same data but formatted as UTF-8 format
     */
public static String inLineDataParser(String inLineData) {
        //replaces all whitespace and non-visible character from the inline data
        inLineData = inLineData.replaceAll("\\s", "");
        String param = "";
        if (inLineData.charAt(0)=='{'){
            inLineData = inLineData.substring(1, inLineData.length()-1);
        }
        String[] args_arrayStrings = inLineData.split("&|,|\n");
        try{
            for (String s: args_arrayStrings){
                String[] each_args_arrayStrings = s.split("=|:");
                for (String s1: each_args_arrayStrings){
                    if (s1.charAt(0)=='"'){
                        s1 = s1.substring(1, s1.length()-1);
                    }
                    param = param.concat(URLEncoder.encode(s1, "UTF-8"));
                    param = param.concat("=");
                }
                param = param.substring(0, param.length()-1);
                param = param.concat("&");
            }
        }catch (Exception e){
            System.out.println("Exception in inLineDataParser.\n"+e.getMessage());
        }
        return param.substring(0, param.length() - 1);
        }   
    
    /**
     * This method read data from the file and puts it in the inLineData variable.
     * @param filePath
     * @return a string containing the data from the file
     */
    public static String readingFromFile(String filePath) {
        String line_ = "";
        try{
            File file = new File(filePath);
            BufferedReader input_file = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line = input_file.readLine()) != null) {
                line_ = line_.concat(line);
            }
            input_file.close();
        }catch(Exception e){
            System.out.println("Exception in readingFromFile!!!"+e.getMessage());
        }
        return line_;
    }
    
    /**
     * This method creates the message that is to be sent over by the socket. 
     * @param requestType either GET or POST
     * @param arguments everything after the .org/"..." or .com/"..."
     * @param hasHeader add to the message only if headers are provided.
     * @param hasData only for the post request. If it has data add it to the message.
     * @return a string that is ready to be send over the socket.
     */
    public static String createMessage(String requestType, String arguments, boolean hasHeader, boolean hasData) {
        String message = "";
        final String HTTP = (" HTTP/1.0\r\n");
        if (requestType=="GET /") {
            message = "GET "+outsideDirectory+"/"+arguments+HTTP+"\r\n";
            if (hasHeader){
                message = message.replace("\r\n\r\n", ("\r\n"+headerData+"\r\n"));
            }
        } else {
            message = requestType+arguments+HTTP;
            message = message.concat("Content-Length: "+inLineData.length()+"\r\n");
            message = message.concat(headerData+"\r\n");
            // if (!hasHeader){
            //     message = message.concat("\r\n");
            // }else{
            //     message = message.concat(headerData+"\r\n");
            // }
            if(hasData){
                message = message.concat(inLineData+"\r\n");
            }
        }
        message = message.concat("\r\n");
       // System.out.println(message);
        return message;
    }
    
    /**
     * This is a common method that can be called for both get and post requests.
     */
    public static void sendMessage(String messageBuilder){
        //from UCPClient
        try(DatagramChannel channel = DatagramChannel.open()){
            int routerAddr= 3000;
            String msg = messageBuilder;
            InetAddress address = InetAddress.getByName("localhost");
            SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(1)
                    .setPortNumber(8007)
                    .setPeerAddress(address)
                    .setPayload(messageBuilder.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddress);

            //System.out.println("Sending to router at "+ routerAddr+"\n----------------\n"+ msg+"\n----------------");

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            //System.out.println("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                System.out.println("No response after timeout");
                throw new Exception("No response after timeout");
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            if(!isVerbose){
                System.out.print("\n");
                String[] lines = payload.split(System.getProperty("line.separator"));
                int count=0;
                for(int i=0;i<lines.length;i++){
                    if(lines[i].length()==1){
                        count = i+1;
                        break;
                    }
                }
                for(int i=count;i<lines.length;i++){
                    System.out.println(lines[i]);
                }
                isVerbose=false;
            }else{
                System.out.print(payload);
            }
            keys.clear();
            AKW = true;

        }
        catch (Exception e) {
        //     System.out.println("ERROR from the sendMessage method.\n"+e.getMessage());
        }

    }

    /**
     * Prints the help menu.
     */
    public static void help(){
        String help = "\nhttpc help\n" 
                +"\nhttpc is a curl-like application but supports HTTP protocol only.\n"
                +"Usage:\n"
                +"\t httpc command [arguments]\n"
                +"The commands are:\n"
                +"\t get \t executes a HTTP GET request and prints the response.\n"
                +"\t post \t executes a HTTP POST request and prints the response.\n"
                +"\t help \t prints this screen.\n"
                +"\nUse \"httpc help [command]\" for more information about a command.\n";

        String help_get = "\nhttpc help get\n"
                +"\nusage: httpc get [-v] [-h key:value] URL\n"
                +"\nGet executes a HTTP GET request for a given URL.\n"
                +"\n-v Prints the detail of the response such as protocol, status, and headers.\n"
                +"-h key:value Associates headers to HTTP Request with the format 'key:value'.\n";

        String help_post = "\nhttpc help post\n"
                +"\nusage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n"
                +"\nPost executes a HTTP POST request for a given URL with inline data or from file.\n"
                +"\n-v Prints the detail of the response such as protocol, status, and headers.\n"
                +"-h key:value Associates headers to HTTP Request with the format 'key:value'.\n"
                +"-d string Associates an inline data to the body HTTP POST request.\n"
                +"-f file Associates the content of a file to the body HTTP POST request.\n"
                +"\nEither [-d] or [-f] can be used but not both.\n";

        if (isPostRequest){
            System.out.println(help_post);
            System.exit(0);
        }else if (isGetRequest){
            System.out.println(help_get);
            System.exit(0);
        }else{
            System.out.println(help);
            System.exit(0);
        }
    }
    
    /**
     * Executes HTTP GET request for a given URL
     */
    public static void get(String inpuString){
        urlParser(inpuString);  

        messagBuilder = createMessage("GET /", arguments, hasHeaderData, false);
        
        //sendMessage(messagBuilder);        
    }
    
    /**
     * Executes a HTTP POST request for a given URL with inline data or from file.
     */
    public static void post(String inpuString){
        urlParser(inpuString);
        
        if (hasInLineData && readFromFile){
            System.out.println("Cannot have -d and -f together. Exiting the application.");
            System.exit(1);
        }
        else if (readFromFile){
            hasInLineData = true;
            inLineData = readingFromFile(filePath);
            inLineData = inLineDataParser(inLineData);
        }
        else if (hasInLineData){
            inLineData = inLineDataParser(inLineData);
        }

        messagBuilder = createMessage("POST /", arguments, hasHeaderData, hasInLineData);

        //sendMessage(messagBuilder);
    }
}