import java.net.*;
import java.io.*;
import java.lang.Integer;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Logger;

/*
httpfs acts as the continously listening serverSocket which accepts any connection and then creates a seperate thread
to take care of that request. (thus we enable the feature of simultaniously managing multiple requests *bonus feature)
*/
public class httpfs {
    private static boolean isVerbose = false;
    private static String portNumber = "8080";
    private static String currentDirectory = "./cwd";
    private static Logger LOGGER = Logger.getLogger("httpfs logger:");

    /*
     * main where we create our ServerSocket and listen for requests, creating
     * threads for each request
     */
    public static void main(String[] args) throws IOException {

        if (args.length >= 6) {
            System.err.println("\nEnter \"httpfs help\" to get more information.\n");
            System.exit(1);
        } else {
            cmdParser(args);
        }
        System.out.println("Server has been instantiated at port " + portNumber+"\n");
        transport obj = new transport("server");
        while (true) {
            String payload = obj.listen();
            
            // System.out.println("server listen done");
            // System.out.println("payload in httpfs.java is "+payload);
            String newPayload = new httpfsThread().start(payload);
            if (isVerbose){
                int i = 0;
                String[] headerInVerbose = newPayload.split("\n");
                while (!headerInVerbose[i].equalsIgnoreCase("\r")){
                    System.out.println(headerInVerbose[i]);
                    i++;
                }
            }
            obj.sendData("server", 41830, newPayload);
            obj.terminatingHandShake();
            System.out.println("\nRequest processed. Waiting for another request.");
        }

        // //create an object of transport() and initialize channel
        // transport obj = new transport("server");
        // //System.out.println("server listen");
        // //call initalHandShake()
        // //obj.listen() to wait for a request from the client
        // String payload = obj.listen();
        // System.out.println("server listen done");
        // //System.out.println("helloooooo\n"+payload+"\nhellooooooo");
        // //get the request and creates the payload for the client
        // //  this is where we call specific methods of httpfs
        // //  the thread is supposed to return the payload for client
        // //call obj.sendData(payload, peerPort) to send the payload to client
        // //call terminatingHandShake()
        // System.out.println("payload in httpfs.java is "+payload);
        // String newPayload = new httpfsThread().start(payload);
        // System.out.println("sending \n"+newPayload);
        // System.out.println("server going to sendData");
        // //System.out.println(newPayload);
        // obj.sendData("server", 41830, newPayload);
        // obj.terminatingHandShake();
    }

    /**
     * This method takes the cmd args and parses them according to the different
     * conditions of the application.
     * 
     * @param args an array of the command line arguments.
     */
    public static void cmdParser(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-v")) {
                isVerbose = true;
            } else if (args[i].equalsIgnoreCase("-p")) {
                portNumber = args[i + 1];
                i++;
            } else if (args[i].equalsIgnoreCase("-d")) {
                currentDirectory = (args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("help")) {
                help();
            }
        }
    }

    /**
     * Prints the help menu.
     */
    public static void help() {
        String help = "\nhttpfs is a simple file server.\n" + "\nUsage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n\n"
                + "  -v  Prints debugging messages.\n"
                + "  -p  Specifies the port number that the server will listen and serve at." + "Default is 8080.\n"
                + "  -d  Specifies the directory that the server will use to read/write"
                + "requested files. Default is the current directory when launching the application.\n";

        System.out.println(help);
        System.exit(0);
    }

    /*
     * httpfsThread is a thread created by httpfs when a connection is accepted. In
     * the thread we will
     */
    private static class httpfsThread extends Thread {

        // private static Socket socket = null;
        // private static BufferedWriter out = null;
        private static BufferedReader in = null;
        private static String[] requestParser = new String[3];
        private static String requestType = "";
        private static String pathFromClient = "";
        private static String http = "";
        private static String statusCode = "";
        private static String headerInfo = "";
        private static String dataFromClient = "";
        private static String bodyForClient = "";
        private static String completeMessage = "";
        private static String timeStamp = "";
        private static boolean overWrite = true;

        /**
         * This is the constrcutor that initialises all the variables for every requests receieved.
         * @param Socket
         */
        public httpfsThread() {
            super();
            requestType = "";
            pathFromClient = "";
            http = "";
            statusCode = "";
            headerInfo = "";
            dataFromClient = "";
            bodyForClient = "";
            completeMessage = "";
            timeStamp = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss").format(Calendar.getInstance().getTime());
        }

        /**
         * This method is the starting point of every thread.
         */
        public String start(String payload) {
            try {
                // System.out.println(payload);
                String response = payload.substring(0,payload.indexOf("\r\n"));
                // response = in.readLine();
                requestParser = response.split(" ");
                requestType = requestParser[0];
                pathFromClient = requestParser[1];
                http = requestParser[2];

                //we take the first line of the request and split it to get what kind of 
                //request it is and pass to requestProcesser()
                requestProcessor(requestType, pathFromClient, payload);

                //once all the processing is finished the "completeMessage" is send to the
                //client and socket is closed.
                // out.write(completeMessage);
                // out.flush();
                // socket.shutdownOutput();
                
                // StringBuilder log = new StringBuilder(socket.getInetAddress().toString()+":");
                // log.append(socket.getLocalPort()+" ");
                // log.append(response+" ");
                // log.append(statusCode.toString()+" ");
                // log.append(bodyForClient.getBytes("UTF-8").length);
                // System.out.println(log);
                // if (httpfs.isVerbose){
                //     httpfs.LOGGER.info(log.toString()+"\n");
                // }
                // BufferedWriter br = new BufferedWriter(new PrintWriter(new FileWriter("./cwd/log.txt", true)));
                // br.write("["+timeStamp+"] "+log.toString()+"\n");
                // br.flush();
                // br.close();
                // socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //System.out.println(completeMessage);
            return completeMessage;
        }

        /**
         * We get the header from the message receieved from the client and data if it's a post request.
         * Then deciphers what kind of request it is and calls the respective functions.
         * Once we are done with processing we call the createMessage() method that creates the 
         * message to be sent to the client.
         * @param requestType
         * @param pathFromClient
         */
        private static void requestProcessor(String requestType, String pathFromClient, String payload) {

            getAdditionalHeader_Data(payload);
            processHeader(headerInfo);
            if (requestType.equals("GET")) {
                get(pathFromClient);
            } else if (requestType.equals("POST")) {
                post(pathFromClient, dataFromClient);
            } else {
                statusCode = "400 BAD REQUEST";
            }

            createMessage(http, statusCode, headerInfo, bodyForClient);
        }


        /**
         * This mehtod is used to get all the information from the client's message and store it.
         */
        private static void getAdditionalHeader_Data(String payload) {
            String response = "";
            boolean hasHeader = true;
            boolean hasData = true;
            Reader inputString = new StringReader(payload);
            in = new BufferedReader(inputString);

            try {
                while (in.ready() && hasHeader) {
                    response = in.readLine();
                    if (response.isEmpty()) {
                        hasHeader = false;
                    } else {
                        headerInfo = headerInfo.concat(response);
                        headerInfo = headerInfo.concat("\n");
                    }
                }
                while (in.ready() && hasData) {
                    response = in.readLine();
                    if (response.isEmpty()) {
                        hasData = false;
                    } else {
                        dataFromClient = dataFromClient.concat(response);
                        dataFromClient = dataFromClient.concat("\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * This method has one functionality for this assignment.
         * It reads through all the headers from the client to check for overwrite option when posting.
         * @param headerInfo
         */
        private static void processHeader(String headerInfo) {
            Scanner s = new Scanner(headerInfo);
            s.useDelimiter("\n");
            while (s.hasNextLine()){
                String eachLine = s.nextLine();
                if (eachLine.contains("overwrite")){
                    String[] postInfo = eachLine.split(":");
                    overWrite = Boolean.valueOf(postInfo[1]);
                }
            }
            s.close();
        }


        /**
         * This mehtod is used to create the reply that will be sent back to the client.
         * @param http
         * @param statusCode
         * @param headerInfo
         * @param bodyForClient
         */
        private static void createMessage(String http, String statusCode, String headerInfo, String bodyForClient) {
            completeMessage = http + " " + statusCode + "\nDate: " + timeStamp +"\r\n\r\n";
            if (headerInfo.length() != 0) {
                completeMessage = completeMessage.replace("\r\n\r\n", ("\r\n" + headerInfo + "\r\n"));
            }
            if (bodyForClient.length() != 0) {
                completeMessage = completeMessage.concat(bodyForClient + "\r\n");
            }
             //System.out.println("completeMessage: \n" + completeMessage);
        }


        /**
         * This method performs a get request. If the pathToDir is a driectory it returns list of
         * files and folders it contains, otherwise it returns the contents of the file.
         * @param pathToDir
         */
        private static void get(String pathToDir) {
            bodyForClient = "";
            int content_len = 0;
            if (!secureAccess(pathFromClient)) {
                return;
            }
            pathToDir = (currentDirectory + pathToDir);
            Path dir = Paths.get(pathToDir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path file : stream) {
                    statusCode = "200 OK";
                    bodyForClient = bodyForClient.concat(file.getFileName().toString() + "\n");
                }
            } catch (IOException | DirectoryIteratorException x) {
                File file = dir.toFile();
                if (file.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();
                        while (line != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = br.readLine();
                        }
                        statusCode = "200 OK";
                        bodyForClient = sb.toString();
                        content_len = bodyForClient.length();
                        headerInfo = headerInfo.concat("Content-Length: "+content_len+"\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    statusCode = "404 NOT FOUND";
                }
            }
        }


        /**
         * This method performs a post request. If the provided path is an exisiting directory then it 
         * returns a 400 BAD REQUEST, otherwise creates all the folders/subfolders and the file
         * and write the DataInfoFromClient into the file.
         * @param pathToDir
         * @param dataFromClient
         */
        private static void post(String pathToDir, String dataFromClient) {
            Path parentPath = null;
            File parentFile = null;
            BufferedWriter br = null;

            if (!secureAccess(pathFromClient)) {
                return;
            }

            pathToDir = (currentDirectory + pathToDir);
            Path dir = Paths.get(pathToDir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                statusCode = "400 BAD REQUEST";
            } catch (IOException | DirectoryIteratorException x) {
                do {
                    parentPath = dir.getParent();
                    parentFile = parentPath.toFile();
                    if (!parentFile.isDirectory()) {
                        parentFile.mkdirs();
                        continue;
                    } else {
                        break;
                    }
                } while (true);
                File file = dir.toFile();
                try {
                    if (overWrite){
                        br = new BufferedWriter(new PrintWriter(new FileWriter(file, false)));
                    }else{
                        br = new BufferedWriter(new PrintWriter(new FileWriter(file, true)));
                    }
                    statusCode = "200 OK";
                    br.write(dataFromClient);
                    br.flush();
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * This method returns a boolean if the user passes ".." right after the hostName.
         * It indicates that the user is trying to access the parent directory which is access violation.
         * @param pathToDir
         */
        private static boolean secureAccess(String pathFromClient) {

            String[] splitPathToDir = pathFromClient.split("/");
            if ((splitPathToDir.length > 0) && (splitPathToDir[1].equals(".."))) {
                statusCode = "403 FORBIDDEN";
                return false;
            }
            return true;
        }
  
    }
}
