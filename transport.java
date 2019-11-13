import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.management.Query;

public class transport{

    String from = "";
    int routerAddr= 3000;
    InetAddress address = InetAddress.getByName("localhost");
    SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
    int serverPort = 8007;
    int clientPort;//extract in the handshake and store it for further use
    Packet packet;
    Queue packetQueue;
    boolean isClient = false;
    boolean isServer = false;
    DatagramChannel channel ;
    LinkedList window;//size should be 3; contains packet objects and a boolean flag

    //constructor
    //serverPortNumber=8007
    //clientPortNumber=41830
    public transport(String from, String portNumber){
        this.from = from;
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(Integer.parseInt(portNumber)));
        } catch (Exception e) {
            //TODO: handle exception
        }
    }


    //this method is called firstly to establish the connection
    //add logger/sysout to keep track of the error messages and drop packets
    public static void handShake(String handShakeFrom) {
        if (handShakeFrom.equalsIgnoreCase("httpc.class")){
            //call createPacket as type SYN and create a client channel
            //and send the packet through the router.

            //now wait for a response from server. channel.receive()
            //this should be a SYN-ACK packet type and sends it back to server

        }
        else{ //this is for the server
            //here we will have the server channel listening
            //if this packet is of type SYN call createPacket which
            //create a packet of type SYN-ACK and sends it back to the client
            //and is listening for his SYN-ACK
        }
    }


    //this method marks the end of the connection
    public static void termiantingHandShake() {
        
    }


    //this method takes in the payload and calculates the numberOfPacketsNeeded
    //and returns a list of n packets.
    public static Queue<Packets> queueOfPackets() {
        Queue<Packet> qPackets = new Query() {
        };
        //call create packet for each of the packet that we need to create
        return lPackets;
    }


    //this method sends packet
    public static void sendData(String sendFrom, int peerPort, String payload) {
        boolean SRDone = false;
        //call queueOfPackets to get a queue of packets
        // while (SRDone is not done)
        //  while (Q is not empty and LinkedList not full) 
        //      populate the linkedList and set flag to false
        //  for eachPacket in LinkedList
        //      send through the channel
        //  keep listening (use method from TA repo)
        //  while(we have received packets)
        //      check the packetType (security reasons)
        //      set flag to True for the recvd packets
        //  reorganize the LinkedList: remove elements until the first element is flase
        //  if(Q is empty and LinkedList is empty)
        //      SRDone=true;
        // create a endOfMessagePacket and send it.
    }


    //this method creates a packet and adds it to either clientQueue
    //or serverQueue depending on who made the request
    public static Packet createPacket() {
        
    }
    

    //this method listens through the channel and sends ACK back
    public static void listen() {
        
    }

}