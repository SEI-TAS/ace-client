package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import org.eclipse.californium.core.CoapClient;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.as.Token;
import se.sics.ace.coap.client.DTLSProfileRequests;

import java.net.InetSocketAddress;
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
        CBORObject params = CBORObject.NewMap();
        params.Add(Constants.GRANT_TYPE, Token.clientCredentials);
        params.Add(Constants.SCOPE, CBORObject.FromObject(scopes));
        params.Add(Constants.AUD, CBORObject.FromObject(audience));

        CBORObject reply = sendRequest("token", "post", params);

        // Convert all int keys back into string keys to process them more easily.
        return Constants.unabbreviate(reply);
    }

    // Posts a token over a non-DTLS secured channel.
    public CBORObject postToken(CBORObject newToken)
    {
        return sendRequestToRS("authz-info", "post", newToken, null);
    }

    // Sends a COAP/DTLS request to the server we are configured to connect to.
    public CBORObject sendRequestToRS(String resource, String method, CBORObject payload, byte[] popKeyId)
    {
        if(popKeyId != null)
        {
            // If the token with a PSK was previously sent, we need to set the kid.
            coapClient = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), popKeyId, keyStructure);
        }
        else
        {
            // If the token has not been sent, we need a non-DTLS connection.
            coapClient = new CoapClient();
            useDTLS = false;
        }

        return sendRequest(resource, method, payload);
    }
}
