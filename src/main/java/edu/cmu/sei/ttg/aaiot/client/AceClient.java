package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.as.Token;
import se.sics.ace.coap.client.DTLSProfileRequests;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-03-17.
 */
public class AceClient extends CoapsPskClient
{
    private OneKey keyStructure;

    public AceClient(String clientId, String serverName, int port, OneKey keyStructure)
    {
        super(serverName, port, clientId, keyStructure.get(KeyKeys.Octet_K).GetByteString());
        this.keyStructure = keyStructure;
    }

    public Map<String, CBORObject> getAccessToken(String scopes, String audience) throws AceException
    {
        Map<String, CBORObject> params = new HashMap<>();
        params.put("grant_type", Token.clientCredentialsStr);
        params.put("scope", CBORObject.FromObject(scopes));
        params.put("aud", CBORObject.FromObject(audience));

        // Convert all string keys into ints as defined in ACE standard.
        CBORObject cborParams = Constants.abbreviate(params);

        CBORObject reply = sendRequest("token", "post", cborParams);

        // Convert all int keys back into string keys to process them more easily.
        return Constants.unabbreviate(reply);
    }

    public CBORObject postToken(CBORObject newToken, String popKeyId)
    {
        return sendRequestToRS("authz-info", "post", newToken, null, popKeyId);
    }

    // Sends a COAP/DTLS request to the server we are configured to connect to.
    public CBORObject sendRequestToRS(String resource, String method, CBORObject payload, CBORObject token, String popKeyId)
    {
        // If we have a token, use it. Check if we are posting it the first time or not, to send it or just the id as the psk-identity field in the DTLS handshake.
        if(token != null)
        {
            // Gets a special client that can send the token as its identity in a request for a RS. Complies with ACE DTLS profile.
            coapClient = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), token, keyStructure);
        }
        else
        {
            // If the token was previously sent, we need to set the kid.
            coapClient = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), popKeyId, keyStructure);
        }

        return sendRequest(resource, method, payload);
    }
}
