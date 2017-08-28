package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileCredentialStore;
import edu.cmu.sei.ttg.aaiot.credentials.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.client.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import se.sics.ace.AceException;
import se.sics.ace.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller
{
    private static final String CONFIG_FILE = "config.json";

    private static final String DEFAULT_RS_IP = "localhost";
    private static final int DEFAULT_RS_PORT = 5685;

    private static final int DEFAULT_AS_PORT = 5684;

    private ICredentialStore credentialStore;

    private CBORObject token = null;
    private String popKeyId = null;
    private OneKey popKeyForRS = null;

    private boolean tokenSent = false;

    public void run() throws COSE.CoseException, IOException, AceException
    {
        Config.load(CONFIG_FILE);

        credentialStore = new FileCredentialStore(Config.data.get("credentials_file"));

        Scanner scanner = new Scanner(System.in);
        while(true) {
            try
            {
                System.out.println("");
                System.out.println("Choose (p)air, (t)oken request, (r)esource request, or (q)uit: ");
                char choice = scanner.nextLine().charAt(0);

                switch (choice)
                {
                    case 'p':
                        boolean success = pair();
                        if (success)
                        {
                            System.out.println("Finished pairing process!");
                        }
                        else
                        {
                            System.out.println("Pairing aborted.");
                        }
                        break;
                    case 't':
                        System.out.println("Input the resource audience to request a token for: ");
                        String rsId = scanner.nextLine();
                        System.out.println("Input the resource scope(s) to request a token for, separated by space: ");
                        String scopes = scanner.nextLine();
                        requestToken(rsId, scopes);
                        break;
                    case 'r':
                        System.out.println("Input resource server's IP, or (Enter) to use default (" + DEFAULT_RS_IP + "): ");
                        String rsIP = scanner.nextLine();
                        if (rsIP.equals(""))
                        {
                            rsIP = DEFAULT_RS_IP;
                        }
                        System.out.println("Input resource server's port, or (Enter) to use default (" + DEFAULT_RS_PORT + "): ");
                        String rsPort = scanner.nextLine();
                        int rsPortInt = DEFAULT_RS_PORT;
                        if (!rsPort.equals(""))
                        {
                            rsPortInt = Integer.parseInt(rsPort);
                        }

                        System.out.println("Input the resource name: ");
                        String resourceName = scanner.nextLine();
                        requestResource(rsIP, rsPortInt, resourceName);
                        break;
                    case 'q':
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid command.");
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error processing command: " + ex.toString());
            }
        }
    }

    public boolean pair() throws IOException
    {
        try
        {
            byte[] key128 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
            PairingResource pairingManager = new PairingResource(Config.data.get("id"), key128, "", credentialStore);
            pairingManager.startPairing();

            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error pairing: " + ex.toString());
            return false;
        }
    }

    public void requestToken(String rsName, String rsScopes) throws COSE.CoseException, IOException, AceException
    {
        if(credentialStore.getASPSK() == null)
        {
            System.out.println("Client not paired yet.");
            return;
        }

        Client asClient = new Client(Config.data.get("id"), credentialStore.getASIP().getHostAddress(), DEFAULT_AS_PORT, credentialStore.getASPSK(),
                null, null, tokenSent);
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScopes, rsName);
        if(reply != null) {
            token = reply.get("access_token");
            System.out.println("Token :" + token);
            tokenSent = false;

            CBORObject popKey = reply.get("cnf");
            System.out.println("Cnf (pop) key: " + popKey);

            CBORObject popKeyData = popKey.get(Constants.COSE_KEY_CBOR);
            System.out.println("Cnf (pop) key data: " + popKeyData);

            CBORObject kidCbor = popKeyData.get(KeyKeys.KeyId.AsCBOR());
            popKeyId = new String(kidCbor.GetByteString(), Constants.charset);
            System.out.println("Cnf (pop) key id: " + popKeyId);

            popKeyForRS = new OneKey(popKeyData);
        }
        asClient.stop();
    }

    public void requestResource(String rsIP, int port, String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(popKeyForRS == null || token == null) {
            System.out.println("Token and POP not obtained yet.");
            return;
        }

        Client rsClient = new Client(Config.data.get("id"), rsIP, port, popKeyForRS, token, popKeyId, tokenSent);
        Map<String, CBORObject> response = rsClient.sendRequest(rsResource, "get", null);
        if(response != null)
        {
            tokenSent = true;
        }
        rsClient.stop();
    }

}
