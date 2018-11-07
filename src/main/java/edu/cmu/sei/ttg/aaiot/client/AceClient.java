/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileASCredentialStore;
import edu.cmu.sei.ttg.aaiot.credentials.IASCredentialStore;
import edu.cmu.sei.ttg.aaiot.network.CoapException;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import edu.cmu.sei.ttg.aaiot.tokens.FileTokenStorage;
import edu.cmu.sei.ttg.aaiot.tokens.IRemovedTokenTracker;
import edu.cmu.sei.ttg.aaiot.tokens.TokenInfo;
import edu.cmu.sei.ttg.aaiot.tokens.RevokedTokenChecker;
import se.sics.ace.AceException;

import javax.naming.NoPermissionException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class AceClient implements IRemovedTokenTracker
{
    // Static key used for pairing with an AS.
    private static final byte[] PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final byte[] TEST_CLIENT1_KEY = {0x61, 0x62, 0x63, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
    private static final byte[] TEST_CLIENT2_KEY = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
    private static final byte[] TEST_CLIENT4_KEY = {0x51, 0x52, 0x53, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};

    // Config file used to load configurations.
    private static final String CONFIG_FILE = "./config.json";

    // Default connection info for RS.
    public static final String DEFAULT_RS_IP = "localhost";
    public static final int DEFAULT_RS_COAP_PORT = 5683;
    public static final int DEFAULT_RS_COAPS_PORT = 5684;

    // Default COAPS port, used for AS.
    public static final int DEFAULT_AS_COAPS_PORT = 5685;

    // Stores AS credentials.
    private IASCredentialStore credentialStore;

    // Stores received tokens.
    private FileTokenStorage tokenStore;

    // Thread to handle revocation checks.
    private RevokedTokenChecker tokenChecker;

    // The client ID.
    private String clientId;

    // Stores known servers and their tokens.
    private Map<String, TokenInfo> resourceServers;

    // Stores list of revoked tokens in this last session.
    private Map<String, String> revokedTokens;

    // Singleton.
    private static AceClient aceClient = null;

    /**
     * Constructor, private for singleton.
     * @throws COSE.CoseException
     * @throws IOException
     */
    private AceClient() throws COSE.CoseException, IOException
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

        // TODO: For testing purposes only:
        //credentialStore.storeAS("AS", TEST_CLIENT4_KEY, InetAddress.getByName("127.0.0.1"));
    }

    /**
     * Singleton getter.
     * @return
     * @throws COSE.CoseException
     * @throws IOException
     */
    public static AceClient getInstance() throws COSE.CoseException, IOException
    {
        if(aceClient == null)
        {
            aceClient = new AceClient();
        }

        return aceClient;
    }

    /**
     * Enables paring to be started by AS.
     * @return
     */
    public boolean enableAndWaitForPairing()
    {
        try
        {
            System.out.println("Creating pairing resource.");
            PairingResource pairingResource = new PairingResource(PAIRING_KEY, clientId,"", credentialStore);
            System.out.println("Starting pairing resource.");
            boolean success = pairingResource.pair();
            if(success)
            {
                // Stop token checker, since info may have changed.
                if(tokenChecker != null)
                {
                    stopRevocationChecker();
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
     * @throws AceException
     */
    public boolean requestToken(String rsName, String rsScopes, String asIp, int port)
            throws AceException, NoPermissionException, UnknownHostException, CoapException
    {
        if(credentialStore.getASPSK() == null)
        {
            System.out.println("Client not paired yet.");
            return false;
        }

        byte[] keyBytes = credentialStore.getASPSK().get(KeyKeys.Octet_K).GetByteString();
        AceCoapClient asClient = new AceCoapClient(InetAddress.getByName(asIp).getHostAddress(), port, clientId, keyBytes);

        try
        {
            Map<String, CBORObject> reply = asClient.getAccessToken(rsScopes, rsName);
            if (reply != null)
            {
                resourceServers.put(rsName, new TokenInfo(rsName, reply));
                tokenStore.storeToFile();
            }
        }
        finally
        {
            asClient.stop();
        }

        return true;
    }

    /**
     * Puts a resource using PUT.
     */
    public String putResource(String rsName, String rsIP, int resourcePort, int authPort, String rsResource, CBORObject payload)
            throws COSE.CoseException, CoapException
    {
        return accessResource("put", rsName, rsIP, resourcePort, authPort, rsResource, payload);
    }

    /**
     * Requests a resource using GET.
     */
    public String requestResource(String rsName, String rsIP, int resourcePort, int authPort, String rsResource)
            throws COSE.CoseException, CoapException
    {
        return accessResource("get", rsName, rsIP, resourcePort, authPort, rsResource, null);
    }

    /**
     * Request a resource and update state.
     * @param rsName
     * @param rsIP
     * @param resourcePort
     * @param rsResource
     * @return
     * @throws COSE.CoseException
     */
    public String accessResource(String method, String rsName, String rsIP, int resourcePort, int authPort, String rsResource, CBORObject payload)
            throws COSE.CoseException, CoapException
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
            AceCoapClient rsClient = new AceCoapClient(rsIP, authPort, null, null);
            try
            {
                // Remove the byte string header from the token.
                CBORObject tokenAsCose = CBORObject.DecodeFromBytes(tokenInfo.token.GetByteString());
                System.out.println("Sending token of type " + tokenAsCose.getType() + ", contents " + tokenAsCose.toString());
                CBORObject response = rsClient.postToken(tokenAsCose);
                tokenInfo.isTokenSent = true;
                tokenStore.storeToFile();
            }
            catch(RuntimeException ex)
            {
                // Exception means server didn't answer or answered with an error.
                System.out.println("Could not post token");
                throw ex;
            }
            finally
            {
                rsClient.stop();
            }
        }
        else
        {
            System.out.println("Token already posted.");
        }

        // Send a request for the resource.
        CBORObject kidCbor = CBORObject.NewMap();
        kidCbor.Add(KeyKeys.KeyId.AsCBOR(), tokenInfo.popKeyId);
        String popKeyId = Base64.getEncoder().encodeToString(kidCbor.EncodeToBytes());
        byte[] popKeyBytes = tokenInfo.popKey.get(KeyKeys.Octet_K.AsCBOR()).GetByteString();
        AceCoapClient rsClient = new AceCoapClient(rsIP, resourcePort, popKeyId, popKeyBytes);

        CBORObject result = rsClient.requestResource(rsResource, method, payload);
        rsClient.stop();

        if(result == null)
        {
            System.out.println("Payload was in unknown format.");
            return "Payload was in unknown format.";
        }

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

    public boolean isCheckingForRevokedTokens()
    {
        return tokenChecker != null;
    }

    /**
     * Starts the revocation thread.
     */
    public boolean startRevocationChecker()
    {
        try
        {
            tokenChecker = new RevokedTokenChecker(credentialStore.getASIP().getHostAddress(), DEFAULT_AS_COAPS_PORT, clientId, credentialStore.getRawASPSK(), this, tokenStore);
            tokenChecker.startChecking();
            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Can't start revoked token checker, no credentials available.");
            tokenChecker = null;
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
