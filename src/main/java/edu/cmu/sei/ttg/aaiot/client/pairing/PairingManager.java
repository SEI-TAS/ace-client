package edu.cmu.sei.ttg.aaiot.client.pairing;

import edu.cmu.sei.ttg.aaiot.network.IMessageHandler;
import edu.cmu.sei.ttg.aaiot.network.UDPClient;
import edu.cmu.sei.ttg.aaiot.network.UDPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class PairingManager implements IMessageHandler
{
    private static final String separator = ":";
    private static final int PORT = 9876;

    private ICredentialStore credentialStore;
    private UDPServer udpServer;

    public PairingManager(ICredentialStore credentialStore)
    {
        this.credentialStore = credentialStore;
    }

    public void startPairing() throws java.io.IOException
    {
        System.out.println("Starting pairing server");
        udpServer = new UDPServer(this, PORT);
        udpServer.waitForMessages();
        System.out.println("Stopped pairing server");
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
                credentialStore.storeAS(asId, Base64.getDecoder().decode(psk));

                // Send id
                String clientId = credentialStore.getId();

                try {
                    UDPClient udpClient = new UDPClient(sourceIP, sourcePort);

                    // Small sleep to give time to the other side to wait for our reply.
                    Thread.sleep(200);
                    System.out.println("Sending reply to port " + sourcePort);
                    udpClient.sendData("a" + separator + "c" + separator + clientId);
                    udpClient.close();

                    System.out.println("Paired!");
                }
                catch(IOException | InterruptedException ex)
                {
                    System.out.println("Error sending message with identity: " + ex.toString());
                }

                // Stop the pairing server.
                udpServer.stop();
        }
    }
}
