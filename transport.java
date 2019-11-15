import java.nio.channels.DatagramChannel;
import java.sql.ClientInfoStatus;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.management.Query;

public class transport{

    String from = "";
    int routerAddr= 3000;
    static InetAddress address = InetAddress.getByName("localhost");
    static SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
    static int serverPort = 8007;
    static int clientPort= 41830;
    Packet packet;
    static Queue packetQueue;
    static boolean isClient = false;
    static boolean isServer = false;
    static DatagramChannel channel ;
    static ArrayList<Packet> window;//size should be 3; contains packet objects and a boolean flag
    static ArrayList<Boolean> windowbool;//size should be 3; contains a boolean flag

    //constructor
    //serverPortNumber=8007
    //clientPortNumber=41830
    public transport(String from){
        try {
            channel = DatagramChannel.open();
        } catch (Exception e) {
            //TODO: handle exception
        }
        if(from.equal("server")){
            isServer=true;
            channel.bind(new InetSocketAddress(serverPort));
        }
        else if(from.equal("client")){
            isClient=true;
            channel.bind(new InetSocketAddress(clientPort));
        }
        //any other cases, breaks the shit down.
    }

//this method sends packet
public static void sendData(String sendFrom, int peerPort, String payload) {
    boolean SRDone = false;
    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
    //call queueOfPackets to get a queue of packets
    packetQueue=queueOfPackets(payload);
    while (!SRDone){
        while (packetQueue.size()!=0 && window.size()!=3){
            //populate the ArrayLis and set flag to false
            window.add((Packet) packetQueue.remove());
            windowbool.add(false);
        }
    for (Packet packet : window){
        //send through the channel
        channel.send(packet.toBuffer(), routerAddress);
    }
    //  keep listening (use method from TA repo)
    channel.configureBlocking(false);
    Selector selector = Selector.open();
    channel.register(selector, 1);
    selector.select(5000);
    Set<SelectionKey> keys = selector.selectedKeys();
    //packets have arrived during the wait time and information about them is stored in keys...
    while(!keys.isEmpty()){
        //check the packetType (security reasons)
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        for(int i=0;i<window.size();i++){
            //set flag to True for the recvd packets
            if(resp.getSequenceNumber==window.get(i).getSequenceNumber){
                windowbool.set(i, true);
            }
        }
    }
    //  reorganize the LinkedList: remove elements until the first element is false
    while(windowbool.size()!=0){
        if(windowbool.get(0)==true){
            window.remove(0);
            windowbool.remove(0);
        }
        else{
            break;
        }
    }
    if(packetQueue.size()==0 && window.size()==0){
        SRDone=true;
    }
    }
    // create a endOfMessagePacket and send it.
    Packet death = createPacket(6, 1, peerPort,"".getBytes());
    channel.send(death.toBuffer(), routerAddress);
    //get response
}

    //this method is called firstly to establish the connection
    //add logger/sysout to keep track of the error messages and drop packets
    public static void handShake(String handShakeFrom) {
        System.out.println("\tInitializing Handshake protocol.\n----------------------");
        if (handShakeFrom.equals("client")){
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, 1);
            Set<SelectionKey> keys = selector.selectedKeys();
            for(int i=0;i<10;i++){
                if(!done){
                    System.out.println("\t***Attempt number: "+(i+1)+"***");
                    //call createPacket as type SYN 
                    Packet p1=createPacket(1, 1, 8007,"".getBytes());
                    //and send the packet through the router.
                    channel.send(p1.toBuffer(), routerAddress);
                    selector.select(5000);
                    //while no response
                    if(keys.isEmpty()){
                        if(i==9){
                            System.out.println("\tMAX attempt number reached.\n\tConnection too unstable\n\tTRY AGAIN LATER");
                            System.exit(1);
                        }
                        continue;
                    }
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    // Parse a packet from the received raw data.
                    buf.flip();
                    Packet resp = Packet.fromBuffer(buf);
                    if(resp.getType==2 && resp.getPayload.equals("".getBytes())){
                        //its a SYN-AKN packet
                        p1 = resp.toBuilder()
                                .create();
                        //created a SYN-AKN packet to send back to server
                        channel.send(p1.toBuffer(), routerAddress);
                        Thread.sleep(3000);
                        done=true;
                    }
                }
            } 
        }
        else{ 
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, 1);
            Set<SelectionKey> keys = selector.selectedKeys();
            while(!done){
                System.out.println("\t***Attempt number: "+count+"***");
                selector.select(5000);
                //while no response
                while(keys.isEmpty()){
                    selector.select(5000);
                    keys = selector.selectedKeys();
                }
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                if(resp.getType==1 && resp.getPayload.equals("".getBytes())){
                    //its a SYN-AKN packet
                    resp = createPacket(2, 1, 41830, "".getBytes());
                    //created a SYN-AKN packet to send back to server
                    channel.send(resp.toBuffer(), routerAddress);
                    channel.configureBlocking(false);
                    selector = Selector.open();
                    channel.register(selector, 1);
                    selector.select(8000);
                    keys = selector.selectedKeys();
                    //while no response
                    if(keys.isEmpty()){
                        //assume after x amount of time, if we got no SYN packet or SYN-AKN that everything is good.
                        done=true;
                    }
                }
                count++;
            }
        }
        System.out.println("----------------------\n\tHandshake Established..."); 
    }


    //this method marks the end of the connection
    public static void termiantingHandShake() {
        //fuck this shit.
    }

    //this method takes in the payload and calculates the numberOfPacketsNeeded
    //and returns a queue of n packets.
    public static Queue<Packets> queueOfPackets(String payload) {
        Queue<Packet> qPackets = new Queue();
        float number=(payload.length()/1013);
        int numbRequired=(int) Math.ceil(number);
        for(int i=1;i<=numbRequired;i++){
            if(isServer){
                if(i=numbRequired){
                    Packet p=createPacket(0,i,clientPort,address,payload.substring(i*(1013), payload.length()));
                    qPackets.add(p);
                }else{
                    Packet p=createPacket(0,i,clientPort,address,payload.substring(i*(1013), (i+1)*(1013)));
                    qPackets.add(p);
                }               
            }
            else{
                if(i=numbRequired){
                    Packet p=createPacket(0,i,serverPort,address,payload.substring(i*(1013), payload.length()));
                    qPackets.add(p);
                }else{
                    Packet p=createPacket(0,i,serverPort,address,payload.substring(i*(1013), (i+1)*(1013)));
                    qPackets.add(p);
                } 
            }
        }
        //call create packet for each of the packet that we need to create
        return qPackets;
    }

    //this method creates a packet and adds it to either clientQueue
    //or serverQueue depending on who made the request
    public static Packet createPacket(int type,long sequenceNumber, int peerPort,byte[] payload) {
        InetAddress address = InetAddress.getByName("localhost");
        Packet p = new Packet.Builder()
                    .setType(type)
                    .setSequenceNumber(sequenceNumber)
                    .setPortNumber(peerPort)
                    .setPeerAddress(address)
                    .setPayload(payload)
                    .create();
        return p;
    }
    

    //this method listens through the channel and sends ACK back
    public static void listen() {
        
    }

}