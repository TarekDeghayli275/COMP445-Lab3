
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Packet represents a simulated network packet.
 * As we don't have unsigned types in Java, we can achieve this by using a larger type.
 */
public class Packet {

    public static final int MIN_LEN = 11;
    public static final int MAX_LEN = 11 + 1024;

    private final int type;
    private final long sequenceNumber;
    private final InetAddress peerAddress;
    private final int peerPort;
    private final byte[] payload;


    public Packet(int type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.payload = payload;
    }

    public int getType() {
        return type;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public InetAddress getPeerAddress() {
        return peerAddress;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Creates a builder from the current packet.
     * It's used to create another packet by re-using some parts of the current packet.
     */
    public Builder toBuilder(){
        return new Builder()
                .setType(type)
                .setSequenceNumber(sequenceNumber)
                .setPeerAddress(peerAddress)
                .setPortNumber(peerPort)
                .setPayload(payload);
    }

    /**
     * Writes a raw presentation of the packet to byte buffer.
     * The order of the buffer should be set as BigEndian.
     */
    private void write(ByteBuffer buf) {
        buf.put((byte) type);
        buf.putInt((int) sequenceNumber);
        buf.put(peerAddress.getAddress());
        buf.putShort((short) peerPort);
        buf.put(payload);
    }

    /**
     * Create a byte buffer in BigEndian for the packet.
     * The returned buffer is flipped and ready for get operations.
     */
    public ByteBuffer toBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        write(buf);
        buf.flip();
        return buf;
    }

    /**
     * Returns a raw representation of the packet.
     */
    public byte[] toBytes() {
        ByteBuffer buf = toBuffer();
        byte[] raw = new byte[buf.remaining()];
        buf.get(raw);
        return raw;
    }

    /**
     * fromBuffer creates a packet from the given ByteBuffer in BigEndian.
     */
    public static Packet fromBuffer(ByteBuffer buf) throws IOException {
        if (buf.limit() < MIN_LEN || buf.limit() > MAX_LEN) {
            throw new IOException("Invalid length");
        }

        Builder builder = new Builder();

        builder.setType(Byte.toUnsignedInt(buf.get()));
        builder.setSequenceNumber(Integer.toUnsignedLong(buf.getInt()));

        byte[] host = new byte[]{buf.get(), buf.get(), buf.get(), buf.get()};
        builder.setPeerAddress(Inet4Address.getByAddress(host));
        builder.setPortNumber(Short.toUnsignedInt(buf.getShort()));

        byte[] payload = new byte[buf.remaining()];
        buf.get(payload);
        builder.setPayload(payload);

        return builder.create();
    }

    /**
     * fromBytes creates a packet from the given array of bytes.
     */
    public static Packet fromBytes(byte[] bytes) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        buf.put(bytes);
        buf.flip();
        return fromBuffer(buf);
    }

    @Override
    public String toString() {
        return String.format("#%d peer=%s:%d, size=%d", sequenceNumber, peerAddress, peerPort, payload.length);
    }

    public static class Builder {
        private int type;
        private long sequenceNumber;
        private InetAddress peerAddress;
        private int portNumber;
        private byte[] payload;

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public Builder setPeerAddress(InetAddress peerAddress) {
            this.peerAddress = peerAddress;
            return this;
        }

        public Builder setPortNumber(int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        public Builder setPayload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Packet create() {
            return new Packet(type, sequenceNumber, peerAddress, portNumber, payload);
        }
    }
}
