package edu.cmu.sei.ttg.aaiot.pairing;

import edu.cmu.sei.ttg.aaiot.network.IMessageHandler;
import edu.cmu.sei.ttg.aaiot.network.UDPClient;
import edu.cmu.sei.ttg.aaiot.network.UDPServer;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class PairingManager implements IMessageHandler
{
    private static final String separator = ":";

    public void startPairing() throws java.io.IOException
    {
        UDPServer server = new UDPServer(this);
        server.waitForMessages();
    }

    @Override
    public void handleMessage(String message, InetAddress sourceIP, int sourcePort)
    {
        String[] parts = message.split(separator);
        String command = parts[0];

        switch(command)
        {
            case "p":
                // Get PSK, AS id
                String asId = parts[1];
                String psk = parts[2];

                // Store PSK

                // Send id
                String clientId = "clientA";
                UDPClient udpClient = new UDPClient(sourceIP, sourcePort);

                try {
                    udpClient.sendData("i" + separator + clientId);
                }
                catch(IOException ex)
                {
                    System.out.println("Error sending message with identity: " + ex.toString());
                }
        }
    }
}
