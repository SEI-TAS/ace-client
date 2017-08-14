package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
//import edu.cmu.sei.ttg.aaiot.client.cc2531.CC2531Controller;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileCredentialStore;
import edu.cmu.sei.ttg.aaiot.credentials.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.client.pairing.PairingManager;
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

    private static final int AS_PORT = 5684;
    private static final int RS_PORT = 5685;

    private ICredentialStore credentialStore;

    private CBORObject token = null;
    private String kid = null;
    private OneKey rsPSK = null;

    private boolean tokenSent = false;

    public void run() throws COSE.CoseException, IOException, AceException //, javax.usb.UsbException
    {
        Config.load(CONFIG_FILE);

        credentialStore = new FileCredentialStore(Config.data.get("credentials_file"));

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (p)air, (t)oken request, (r)esource request, or (q)uit: ");
            char choice = scanner.nextLine().charAt(0);

            switch (choice) {
                case 'p':
                    boolean success = pair();
                    if(success)
                    {
                        System.out.println("Finished pairing process!");
                    }
                    else
                    {
                        System.out.println("Pairing aborted.");
                    }
                    break;
                case 't':
                    System.out.println("Input the resource server id to request a token for: ");
                    String rsId = scanner.nextLine();
                    System.out.println("Input the resource scope(s) to request a token for, separated by space: ");
                    String scopes = scanner.nextLine();
                    requestToken(rsId, scopes);
                    break;
                case 'r':
                    System.out.println("Input resource server's IP, or (d) to use default (" + DEFAULT_RS_IP + "): ");
                    String rsIP = scanner.nextLine();
                    if (rsIP.equals("d"))
                    {
                        rsIP = DEFAULT_RS_IP;
                    }

                    System.out.println("Input the resource name: ");
                    String resourceName = scanner.nextLine();
                    requestResource(rsIP, resourceName);
                    break;
                case 'q':
                    System.exit(0);
                case 'u':
                    System.out.println("USB test");
                   //CC2531Controller cc2531 = new CC2531Controller();
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        }

    }

    public boolean pair() throws IOException
    {
        try
        {
            PairingManager pairingManager = new PairingManager(Config.data.get("id"), this.credentialStore);
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

        Client asClient = new Client(Config.data.get("id"), Config.data.get("AS_IP"), AS_PORT, credentialStore.getASPSK(),
                null, null, tokenSent);
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScopes, rsName);
        if(reply != null) {
            token = reply.get("access_token");
            System.out.println("Token :" + token);
            tokenSent = false;

            CBORObject popKey = reply.get("cnf");
            System.out.println("Cnf: " + popKey);

            CBORObject rsKeyData = popKey.get(Constants.COSE_KEY_CBOR);
            System.out.println("Cnf key: " + rsKeyData);

            CBORObject kidCbor = rsKeyData.get(KeyKeys.KeyId.AsCBOR());
            kid = new String(kidCbor.GetByteString(), Constants.charset);
            System.out.println("Cnf key id: " + kid);

            rsPSK = new OneKey(rsKeyData);
        }
        asClient.stop();
    }

    public void requestResource(String rsIP, String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(rsPSK == null || token == null) {
            System.out.println("Token and POP not obtained yet.");
            return;
        }

        Client rsClient = new Client(Config.data.get("id"), rsIP, RS_PORT, rsPSK, token, kid, tokenSent);
        Map<String, CBORObject> response = rsClient.sendRequest(rsResource, "get", null);
        if(response != null)
        {
            tokenSent = true;
        }
        rsClient.stop();
    }

}
