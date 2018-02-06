package edu.cmu.sei.ttg.aaiot.client;

import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileASCredentialStore;
import edu.cmu.sei.ttg.aaiot.credentials.IASCredentialStore;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import edu.cmu.sei.ttg.aaiot.tokens.FileTokenStorage;
import edu.cmu.sei.ttg.aaiot.tokens.IRemovedTokenTracker;
import edu.cmu.sei.ttg.aaiot.tokens.TokenInfo;
import edu.cmu.sei.ttg.aaiot.tokens.RevokedTokenChecker;
import se.sics.ace.AceException;

import javax.naming.NoPermissionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Manager implements IRemovedTokenTracker
{
    private static final byte[] PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final String CONFIG_FILE = "./config.json";

    public static final String DEFAULT_RS_IP = "localhost";
    public static final int DEFAULT_RS_PORT = 5685;

    private static final int AS_COAP_PORT = 5684;

    private IASCredentialStore credentialStore;
    private FileTokenStorage tokenStore;
    private RevokedTokenChecker tokenChecker;

    private String clientId;
    private Map<String, TokenInfo> resourceServers;
    private Map<String, String> revokedTokens;

    // Singleton.
    private static Manager manager = null;

    /**
     * Constructor, private for singleton.
     * @throws COSE.CoseException
     * @throws IOException
     * @throws AceException
     */
    private Manager() throws COSE.CoseException, IOException, AceException
    {
        try
        {
            Config.load(CONFIG_FILE);
        }
        catch(Exception ex)
        {
            System.out.println("Error loading config file: " + ex.toString());
            throw new RuntimeException("Error loading config file: " + ex.toString());
        }

        clientId = Config.data.get("id");

        credentialStore = new FileASCredentialStore(Config.data.get("credentials_file"));
        tokenStore = new FileTokenStorage();
        resourceServers = tokenStore.getTokens();
        revokedTokens = new HashMap<>();
    }

    /**
     * Singleton getter.
     * @return
     * @throws COSE.CoseException
     * @throws IOException
     * @throws AceException
     */
    public static Manager getInstance() throws COSE.CoseException, IOException, AceException
    {
        if(manager == null)
        {
            manager = new Manager();
        }

        return manager;
    }

    /**
     * Enables paring to be started by AS.
     * @return
     * @throws IOException
     */
    public boolean enableAndWaitForPairing() throws IOException
    {
        try
        {
            PairingResource pairingResource = new PairingResource(PAIRING_KEY, clientId,"", credentialStore);
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

    /**
     * Request a token and store it.
     * @param rsName
     * @param rsScopes
     * @return
     * @throws COSE.CoseException
     * @throws IOException
     * @throws AceException
     */
    public boolean requestToken(String rsName, String rsScopes) throws COSE.CoseException, IOException, AceException, NoPermissionException
    {
        if(credentialStore.getASPSK() == null)
        {
            System.out.println("Client not paired yet.");
            return false;
        }

        AceClient asClient = new AceClient(clientId, credentialStore.getASIP().getHostAddress(), AS_COAP_PORT, credentialStore.getASPSK());
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScopes, rsName);
        if(reply != null)
        {
            resourceServers.put(rsName, new TokenInfo(rsName, reply));
            tokenStore.storeToFile();
        }
        asClient.stop();

        return true;
    }

    /**
     * Request a resource and update state.
     * @param rsName
     * @param rsIP
     * @param port
     * @param rsResource
     * @return
     * @throws COSE.CoseException
     * @throws IOException
     * @throws AceException
     */
    public String requestResource(String rsName, String rsIP, int port, String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(!resourceServers.containsKey(rsName))
        {
            System.out.println("Token and POP not obtained yet for " + rsName);
            throw new IllegalArgumentException("Token and POP not obtained yet for " + rsName);
        }

        // Check if we have sent an access token already.
        TokenInfo tokenInfo = resourceServers.get(rsName);
        if(!tokenInfo.isTokenSent)
        {
            // Post the token.
            System.out.println("Posting token.");
            int rsNonDTLSPort = Integer.parseInt(Config.data.get("rs_non_dtls_port"));
            AceClient rsClient = new AceClient(clientId, rsIP, rsNonDTLSPort, new OneKey(tokenInfo.popKey));
            CBORObject response = rsClient.postToken(tokenInfo.token);
            if(response != null)
            {
                tokenInfo.isTokenSent = true;
                tokenStore.storeToFile();
            }
            rsClient.stop();
        }
        else
        {
            System.out.println("Token already posted.");
        }

        // Send a request for the resource.
        AceClient rsClient = new AceClient(clientId, rsIP, port, new OneKey(tokenInfo.popKey));
        CBORObject result = rsClient.sendRequestToRS(rsResource, "get", null, tokenInfo.popKeyId);
        rsClient.stop();

        if(result == null)
            throw new RuntimeException("Resource server did not reply or stopped the connection.");

        return result.toString();
    }

    /**
     * Enables or disables the token revocation check thread.
     */
    public boolean toggleRevocationChecker()
    {
        if(tokenChecker == null)
        {
            System.out.println("Starting revocation checker");
            return startRevocationChecker();
        }
        else
        {
            System.out.println("Stopping revocation checker");
            stopRevocationChecker();
            return true;
        }
    }

    /**
     * Stops the revocation thread.
     */
    public void stopRevocationChecker()
    {
        if (tokenChecker != null)
        {
            tokenChecker.stopChecking();
            tokenChecker = null;
        }
    }

    /**
     * Starts the revocation thread.
     */
    public boolean startRevocationChecker()
    {
        try
        {
            tokenChecker = new RevokedTokenChecker(credentialStore.getASIP().getHostAddress(), AS_COAP_PORT, clientId, credentialStore.getRawASPSK(), this, tokenStore);
            tokenChecker.startChecking();
            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Can't start revoked token checker, no credentials available.");
            return false;
        }
    }

    public IASCredentialStore getCredentialStore()
    {
        return credentialStore;
    }

    public FileTokenStorage getTokenStore()
    {
        return tokenStore;
    }

    @Override
    public void notifyRemovedToken(String tokenId, String rsId)
    {
        revokedTokens.put(tokenId, rsId);
    }

    public Map<String, String> getRevokedTokens()
    {
        return revokedTokens;
    }
}
