package in.daydreamers.devapps;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Callable;

public class DevAppsTime {

        private static final String NTP_SERVER = "time.google.com";  // NTP server
        private static final int NTP_PORT = 123;                     // NTP port

        public static Long getCurrentTimeFromNTP() {

                    try {
                        InetAddress address = InetAddress.getByName(NTP_SERVER);
                        DatagramSocket socket = new DatagramSocket();
                        socket.setSoTimeout(5000);  // Timeout of 5 seconds
                        byte[] buffer = new byte[48];

                        // Initialize NTP request packet
                        buffer[0] = 0x1B; // NTP version

                        // Send NTP request packet to the server
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);
                        socket.send(request);

                        // Receive response packet from the NTP server
                        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                        socket.receive(response);

                        socket.close();

                        // Extract the timestamp from the NTP response
                        long timestamp = ((buffer[43] & 0xFF) << 24) | ((buffer[42] & 0xFF) << 16) |
                                ((buffer[41] & 0xFF) << 8) | (buffer[40] & 0xFF);

                        // Convert NTP timestamp to milliseconds since Unix epoch
                        long epochMillis = (timestamp - 2208988800L) * 1000L; // NTP epoch starts in 1900, UNIX epoch starts in 1970

                        return epochMillis;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return -1L; // Error in fetching tim
        }
    }

