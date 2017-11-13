package edu.cmu.sei.ttg.aaiot.client;

import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileASCredentialStore;
import edu.cmu.sei.ttg.aaiot.credentials.IASCredentialStore;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import edu.cmu.sei.ttg.aaiot.tokens.FileTokenStorage;
import edu.cmu.sei.ttg.aaiot.tokens.ResourceServer;
import edu.cmu.sei.ttg.aaiot.tokens.RevokedTokenChecker;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller
{
    private static final byte[] PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final String CONFIG_FILE = "config.json";

    private static final String DEFAULT_RS_IP = "localhost";
    private static final int DEFAULT_RS_PORT = 5685;
    private static final int RS_COAP_PORT = 5690;

    private static final int DEFAULT_AS_PORT = 5684;

    private IASCredentialStore credentialStore;
    private FileTokenStorage tokenStore;
    private RevokedTokenChecker tokenChecker;

    private String clientId;
    private Map<String, ResourceServer> resourceServers;

    public void run() throws COSE.CoseException, IOException, AceException
    {
        try
        {
            Config.load(CONFIG_FILE);
        }
        catch(Exception ex)
        {
            System.out.println("Error loading config file: " + ex.toString());
            return;
        }

        clientId = Config.data.get("id");

        credentialStore = new FileASCredentialStore(Config.data.get("credentials_file"));
        tokenStore = new FileTokenStorage();
        resourceServers = tokenStore.getTokens();

        Scanner scanner = new Scanner(System.in);
        while(true) {
            try
            {
                System.out.println("");
                System.out.println("Choose (p)air, (t)oken request, (r)esource request, (v) start/stop revoked tokens check, or (q)uit: ");
                String choiceString = scanner.nextLine();
                if(choiceString == null || choiceString.equals(""))
                {
                    System.out.println("No command selected, try again.");
                    continue;
                }

                char choice = choiceString.charAt(0);
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
                        System.out.println("Input the resource audience to request a token for (RS name): ");
                        String rsId = scanner.nextLine();
                        System.out.println("Input the resource scope(s) to request a token for, separated by space: ");
                        String scopes = scanner.nextLine();
                        requestToken(rsId, scopes);
                        break;
                    case 'r':
                        System.out.println("Input the resource audience to send the request to (RS name): ");
                        String rsName = scanner.nextLine();

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
                        requestResource(rsName, rsIP, rsPortInt, resourceName);
                        break;
                    case 'v':
                        toggleRevocationChecker();
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
                ex.printStackTrace();
            }
        }
    }

    public boolean pair() throws IOException
    {
        try
        {
            PairingResource pairingResource = new PairingResource(PAIRING_KEY, Config.data.get("id"),"", credentialStore);
            boolean success = pairingResource.pair();
            if(success)
            {
                // Restart token checker, since info may have changed.
                if(tokenChecker != null)
                {
                    stopRevocationChecker();
                    startRevocationChecker();
                }
            }
            return success;
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

        AceClient asClient = new AceClient(clientId, credentialStore.getASIP().getHostAddress(), DEFAULT_AS_PORT, credentialStore.getASPSK());
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScopes, rsName);
        if(reply != null)
        {
            resourceServers.put(rsName, new ResourceServer(rsName, reply));
            tokenStore.storeToFile();
        }
        asClient.stop();
    }

    public void requestResource(String rsName, String rsIP, int port, String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(!resourceServers.containsKey(rsName))
        {
            System.out.println("Token and POP not obtained yet for " + rsName);
            return;
        }

        ResourceServer rs = resourceServers.get(rsName);
        if(!rs.isTokenSent)
        {
            // First post token, if it has not be sent before.
            AceClient rsClient = new AceClient(clientId, rsIP, RS_COAP_PORT, new OneKey(rs.popKey));
            CBORObject response = rsClient.postToken(rs.token);
            if(response != null)
            {
                rs.isTokenSent = true;
                tokenStore.storeToFile();
            }
            rsClient.stop();
        }

        // Send a request for the resource.
        AceClient rsClient = new AceClient(clientId, rsIP, port, new OneKey(rs.popKey));
        rsClient.sendRequestToRS(rsResource, "get", null, rs.popKeyId);
        rsClient.stop();
    }

    private void toggleRevocationChecker()
    {
        if(tokenChecker == null)
        {
            System.out.println("Starting revocation checker");
            startRevocationChecker();
        }
        else
        {
            System.out.println("Stopping revocation checker");
            stopRevocationChecker();
        }
    }

    private void stopRevocationChecker()
    {
        if (tokenChecker != null)
        {
            tokenChecker.stopChecking();
            tokenChecker = null;
        }
    }

    private void startRevocationChecker()
    {
        try
        {
            tokenChecker = new RevokedTokenChecker(credentialStore.getASIP().getHostAddress(), DEFAULT_AS_PORT, clientId, credentialStore.getRawASPSK(), tokenStore);
            tokenChecker.startChecking();
        }
        catch(Exception ex)
        {
            System.out.println("Can't start revoked token checker, no credentials available.");
        }
    }

}
