package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.as.Token;
import se.sics.ace.coap.client.DTLSProfileRequests;

import javax.naming.NoPermissionException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Map;

/**
 * Created by Sebastian on 2017-03-17.
 */
public class AceCoapClient extends CoapsPskClient
{
    // Simple constructor.
    public AceCoapClient(String serverName, int port, String keyId, byte[] key)
    {
        super(serverName, port, keyId, key);
    }

    /**
     * Obtains an access token from the AS.
     * @param scopes The list of scopes, separated by spaces, for which the token will be valid.
     * @param audience The audience we want to use this token with.
     * @return A map of the reply of the AS, including the token and the POP key to use to secure DTLS connections.
     * @throws AceException
     * @throws NoPermissionException
     */
    public Map<String, CBORObject> getAccessToken(String scopes, String audience) throws AceException, NoPermissionException
    {
        CBORObject params = CBORObject.NewMap();
        params.Add(Constants.GRANT_TYPE, Token.clientCredentials);
        params.Add(Constants.SCOPE, CBORObject.FromObject(scopes));
        params.Add(Constants.AUD, CBORObject.FromObject(audience));

        CBORObject reply = sendRequest("token", "post", params);
        if(reply == null)
        {
            throw new NoPermissionException("No token received, most likely due to permission issues.");
        }
        else
        {
            // Convert all int keys back into string keys to process them more easily.
            return Constants.unabbreviate(reply);
        }
    }

    /**
     *  Posts a token over a non-DTLS secured channel.
     * @param newToken The token to be posted.
     * @return
     */
    public CBORObject postToken(CBORObject newToken) throws COSE.CoseException
    {
        // Do the actual sending of the COAP(s) request.
        CBORObject reply = sendRequest("authz-info", "post", newToken);
        if(reply == null)
        {
            throw new RuntimeException("No reply received when posting token.");
        }
        else
        {
            return reply;
        }
    }

    /**
     * Sends a COAPS request to the server we are configured to connect to.
     * @param resource The resource/endpoint we want to access.
     * @param method The method (post/get) for the request.
     * @param payload Any payload to be sent with the request (post requests).
     * @return
     */
    public CBORObject requestResource(String resource, String method, CBORObject payload) throws COSE.CoseException
    {
        // Key ID is needed in bytes by helper method.
        CBORObject keyCbor = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(keyId));
        byte[] keyIdBytes = keyCbor.get(KeyKeys.KeyId.AsCBOR()).GetByteString();

        // Format the key as CBOR as requested by helper method.
        CBORObject keyMap = CBORObject.NewMap();
        keyMap.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyMap.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(this.key));
        OneKey fullKey = new OneKey(keyMap);

        // Get DTLS client properly setup for this type of request.
        coapClient = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), keyIdBytes, fullKey);

        // Do the actual sending of the COAP(s) request.
        return sendRequest(resource, method, payload);
    }
}
