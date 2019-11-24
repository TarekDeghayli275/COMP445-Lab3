import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.sql.ClientInfoStatus;
import java.util.*;
import java.net.*; 
import java.io.*;

import javax.management.Query;

public class transport{

    String from = "";
    int routerAddr= 3000;
    static InetAddress address;
    static SocketAddress routerAddress;
    static int serverPort = 8007;
    static int clientPort= 41830;
    static Queue packetQueue;
    static boolean isClient = false;
    static boolean isServer = false;
    static DatagramChannel channel ;
    static ArrayList<Packet> window;//size should be 3; contains packet objects and a boolean flag
    static ArrayList<Boolean> windowbool;//size should be 3; contains a boolean flag
    static ArrayList<String> payloadList;
    static ByteBuffer emptybuf = ByteBuffer.allocate(Packet.MAX_LEN);
    //type 0 = Data
    //type 1 = Syn
    //type 2 = Syn-Ack
    //type 3 = Ack
    //type 5 = fin
    //type 6 = fin-Ack

    //constructor
    //serverPortNumber=8007
    //clientPortNumber=41830
    public transport(String from){
        try {
            channel = DatagramChannel.open();
            address = InetAddress.getByName("localhost");
            routerAddress = new InetSocketAddress("localhost", 3000);
            payloadList = new ArrayList<String>();
            windowbool = new ArrayList<Boolean>();
            window= new ArrayList<Packet>();
            if(from.equals("server")){
                isServer=true;
                channel.bind(new InetSocketAddress(serverPort));
            }
            else if(from.equals("client")){
                isClient=true;
                channel.bind(new InetSocketAddress(clientPort));
            }
            channel.configureBlocking(false);
        } catch (Exception e) {
            //handle exception
        }
        //any other cases, breaks the shit down.
    }

//this method sends packet
    public void sendData(String sendFrom, int peerPort, String payload) {
        try{
            boolean SRDone = false;
            //call queueOfPackets to get a queue of packets
            packetQueue= queueOfPackets(payload);
            //System.out.println("got my queue of size "+packetQueue.size());
            // Selector selector = Selector.open();
            // channel.register(selector, 1);
            // Set<SelectionKey> keys = selector.selectedKeys();
                while (!SRDone) {
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    while (packetQueue.size() != 0 && window.size() != 3) {
                        //System.out.println("adding element to windown.");
                        // populate the ArrayLis and set flag to false
                        window.add((Packet) packetQueue.remove());
                        windowbool.add(false);
                    }
                    long startTime = System.currentTimeMillis();
                    while (buf.equals(emptybuf)){
                        for (Packet packetData : window) {
                            // send through the channel
                            // System.out.println("*************sending a packet with :*************");
                            String temp = new String(packetData.getPayload());
                            // System.out.println(temp);
                            channel.send(packetData.toBuffer(), routerAddress);
                        }
                        startTime = System.currentTimeMillis();
                        while((System.currentTimeMillis()-startTime)<8000 && buf.equals(emptybuf)){
                            channel.receive(buf);
                        }
                    }
                    //System.out.println("got a packet going to check it now");
                    // keep listening (use method from TA repo)
                    // selector.select(5000);
                    // keys = selector.selectedKeys();
                    buf.flip();
                    // packets have arrived during the wait time and information about them is
                    // stored in keys...
                    // while (!keys.isEmpty()) {
                        // check the packetType (security reasons)
                        // channel.receive(buf);
                        // buf.flip();
                        Packet resp = Packet.fromBuffer(buf);
                        for (int i = 0; i < window.size(); i++) {
                            // set flag to True for the recvd packets
                            if (resp.getSequenceNumber() == window.get(i).getSequenceNumber()) {
                                windowbool.set(i, true);
                            }
                        }
                    //}
                    // reorganize the LinkedList: remove elements until the first element is false
                    while (windowbool.size() != 0) {
                        if (windowbool.get(0) == true) {
                            window.remove(0);
                            windowbool.remove(0);
                        } else {
                            break;
                        }
                    }
                    if (packetQueue.size() == 0 && window.size() == 0) {
                        SRDone = true;
                        // keys.clear();
                    }
                }
                // System.out.println("************* got all my akn, going to kill now *************");
                boolean DeathSR = false;
                // create a endOfMessagePacket and send it.
                Packet death = createPacket(5, 1L, peerPort, "");
                while (!DeathSR) {
                    ByteBuffer buf_death = ByteBuffer.allocate(Packet.MAX_LEN);
                    // System.out.println("************* sending death packet *************");
                    channel.send(death.toBuffer(), routerAddress);
                    long startTime = System.currentTimeMillis();
                    while ((System.currentTimeMillis()-startTime)<5000&&buf_death.equals(emptybuf)){
                        channel.receive(buf_death);
                    }
                    //System.out.println("checking");
                    if(!buf_death.equals(emptybuf)){
                        //if packet recd then do this below
                        buf_death.flip();
                        Packet resp_6 = Packet.fromBuffer(buf_death);
                        //System.out.println(resp.getType());
                        //System.out.println(resp.getType() == 6);
                        if (resp_6.getType() == 6) {
                            // System.out.println("************* got the death akn *************");
                            DeathSR = true;
                            Packet deathakn = createPacket(7, 1L, peerPort, "11");
                            channel.send(deathakn.toBuffer(), routerAddress);
                        }
                    }
                }
            // buf.clear();
            // packetQueue.clear();
            // window.clear();
            // windowbool.clear();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    // this method is called firstly to establish the connection
    // add logger/sysout to keep track of the error messages and drop packets
    public void handShake() {
        try{
            boolean done=false;
            System.out.println("\tInitializing Handshake protocol.\n--------------------------------------------");
            channel.configureBlocking(false);
            for (int i = 0; i < 5; i++) {
                if (!done) {
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    System.out.println("\t*** Attempt number: " + (i + 1) + " ***");
                    // call createPacket as type SYN
                    Packet p1 = createPacket(1, 1L, 8007, "");
                    // and send the packet through the router.
                    channel.send(p1.toBuffer(), routerAddress);
                    long startTime = System.currentTimeMillis();
                    while((System.currentTimeMillis()-startTime)<4000 && buf.equals(emptybuf)){
                        channel.receive(buf);
                    }
                    //received someting
                    if(!buf.equals(emptybuf)){
                        buf.flip();
                        Packet resp_2 = Packet.fromBuffer(buf);
                        if(resp_2.getType()==2){
                            Packet p3 = createPacket(3, 1L, 8007, "");
                            channel.send(p3.toBuffer(), routerAddress);//dont realy care if it gets lost.
                            done=true;
                        }
                    }
                }
            }
            if(!done){
                System.out.println("--------------------------------------------\n\tHandshake failed...");
                System.exit(1);
            }
            System.out.println("--------------------------------------------\n\tHandshake Established...");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    //waits 8 seconds just in case FIN-SYN-ACK is lost.
    public void terminatingHandShake() {
        try{
            long startTime = System.currentTimeMillis();
            while((System.currentTimeMillis()-startTime)<8000){
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                long startTime2 = System.currentTimeMillis();
                while((System.currentTimeMillis()-startTime2)<2000 && buf.equals(emptybuf)){
                    channel.receive(buf);
                }
                if(!buf.equals(emptybuf)){
                    buf.flip();
                    Packet packet = Packet.fromBuffer(buf);
                    if(packet.getType()==6){
                        Packet response = createPacket(7, packet.getSequenceNumber(), clientPort , "");
                        channel.send(response.toBuffer(), routerAddress);
                    }
                }
            }
        }catch(Exception e){
            //nothing
        }
    }

    // this method takes in the payload and calculates the numberOfPacketsNeeded
    // and returns a queue of n packets.
    public static Queue<Packet> queueOfPackets(String payload) {
        Queue<Packet> qPackets = new LinkedList<Packet>();
        try{
            double number = (payload.length() / 1013d);
            int numbRequired = (int) Math.ceil(number);
            //System.out.println("number required is "+numbRequired+" to send the whole payload");
            for (int i = 0; i < numbRequired; i++) {
                if (isServer) {
                    if (i == numbRequired-1) {
                        Packet p = createPacket(0, (long)i+1 , clientPort, payload.substring(i * (1013), payload.length())+"\r\n");
                       // System.out.println("the packet payload is "+payload.substring(i * (1013), payload.length())+"\r\n"+"sup");
                        qPackets.add(p);
                    } else {
                        Packet p = createPacket(0, (long)i+1 , clientPort, payload.substring(i * (1013), (i + 1) * (1013)));
                        qPackets.add(p);
                    }
                } else {
                    if (i == numbRequired-1) {
                        Packet p = createPacket(0, (long)i+1 , serverPort, payload.substring(i * (1013), payload.length())+"\r\n");
                       // System.out.println("the fdpacket payload is "+payload.substring(i * (1013), payload.length())+"\r\n"+"sup");
                       
                        qPackets.add(p);
                    } else {
                        Packet p = createPacket(0, (long)i+1, serverPort, payload.substring(i * (1013), (i + 1) * (1013)));
                        qPackets.add(p);
                    }
                }
            }
            // call create packet for each of the packet that we need to create
        }catch(Exception e){
            e.printStackTrace();
        }
        return qPackets;
    }

    // this method creates a packet and adds it to either clientQueue
    // or serverQueue depending on who made the request
    public static Packet createPacket(int type, long sequenceNumber, int peerPort, String payload) {
        byte[] payl=payload.getBytes();
        Packet p = new Packet.Builder()
                    .setType(type)
                    .setSequenceNumber(sequenceNumber)
                    .setPortNumber(peerPort)
                    .setPeerAddress(address)
                    .setPayload(payl)
                    .create();
        return p;
    }
    

    //this method listens through the channel and sends ACK back
    //death = 5
    //send back into 6
    public String listen() {
        int port;
        if(isClient){
            port=8007;
        }else{
            port=41830;
        }
        String payload="";
        try{
            Map<Integer, Integer> table = new Hashtable<Integer, Integer>();
            // String temp="";
            // Selector selector = Selector.open();
            // Set<SelectionKey> keys = selector.selectedKeys();
            boolean SRdone=false;
            while(!SRdone){
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                // Packet resp_0= createPacket(0, 0L, 0, "");
                //System.out.println("in the Selective repeat");
                // selector.select(500);
                // keys = selector.selectedKeys();
                
                // selector.select(500);
                // keys = selector.selectedKeys();
                while (buf.equals(emptybuf)){
                    channel.receive(buf);
                }
                //System.out.println("got a something");
                // channel.receive(buf);
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                // buf.flip();
                String s = new String(packet.getPayload());
                //System.out.println("got a packet, info: \n"+s);
                //make sure we get an Akn for the death.
                if(packet.getType()==5){
                    boolean DeathDeathSR =false;
                    Packet resp_0 = createPacket(6, packet.getSequenceNumber(), port , "1");
                    while(!DeathDeathSR){
                        // System.out.println("************* sending akn for death *************");
                        channel.send(resp_0.toBuffer(), routerAddress);
                        long startTime = System.currentTimeMillis();
                        // buf.clear();
                        buf = ByteBuffer.allocate(Packet.MAX_LEN);
                        while(buf.equals(emptybuf)&&(System.currentTimeMillis()-startTime)<5000){
                            channel.receive(buf);
                        }
                        if(!buf.equals(emptybuf)){
                            buf.flip();
                            Packet death = Packet.fromBuffer(buf);
                            // buf.flip();
                            if(death.getType()==5){
                                channel.send(resp_0.toBuffer(), routerAddress);
                            }
                            //should not be possible
                            else if(death.getType()==6){
                                // System.out.println("type 6 received. Bullshit!!!");
                            }
                            else if(death.getType()==7){
                                // System.out.println("************* got akn for death *************");
                                DeathDeathSR=true;
                                SRdone=true;
                            }
                        }
                        else{
                            channel.send(resp_0.toBuffer(), routerAddress);
                        }
                        //System.out.println("got a something");
                        // channel.receive(buf);
                        // buf.flip();
                    }
                }
                else if(packet.getType()==6){
                    // System.out.println("************* got a type 6 and sending back 7 *************");
                    Packet resp_0 = createPacket(7, packet.getSequenceNumber(), port , "11");
                    channel.send(resp_0.toBuffer(), routerAddress);
                }
                //case of the handshake
                else if(packet.getType()==1){
                    Packet resp_2 = createPacket(2, packet.getSequenceNumber(), port, "");
                    channel.send(resp_2.toBuffer(), routerAddress);
                }
                else if(packet.getType()==3){
                    //do nothing. its a SYN-ACK-ACK;
                }
                //doesnt have this packet before
                else if(!table.containsKey((int)packet.getSequenceNumber())){
                    table.put((int)packet.getSequenceNumber(),(int)packet.getSequenceNumber());
                    //fix the order of received packets.
                    //System.out.println("dont have this packet");
                    int position = (int) packet.getSequenceNumber();
                    // temp=packet.getPayload().toString();
                    if(position > payloadList.size()){
                        //check the index
                        payloadList.add(s);
                        Packet resp_0 = createPacket(3, packet.getSequenceNumber(), port, "");
                        channel.send(resp_0.toBuffer(), routerAddress);
                    }
                    else{
                        payloadList.add(position-1,s);
                        Packet resp_0 = createPacket(3, packet.getSequenceNumber(), port, "");
                        channel.send(resp_0.toBuffer(), routerAddress);
                    }
                    // channel.send(resp_0.toBuffer(), routerAddress);
                    //System.out.println("sending akn for "+(int)packet.getSequenceNumber());
                }else{
                    // System.out.println("************* sending akn for \n"+s);
                    Packet resp_0 = createPacket(3, packet.getSequenceNumber(), port, "");
                    channel.send(resp_0.toBuffer(), routerAddress);
                }
            }
            //System.out.println("done with the selective repeat");
            for (String payl: payloadList) {
                payload= payload + payl;
            }
            //System.out.println("Payload all together is "+payload);
        }catch(Exception e){
            e.printStackTrace();
        }
        return payload;
    }
}