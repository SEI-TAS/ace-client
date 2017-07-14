import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.as.Token;
import se.sics.ace.coap.client.DTLSProfileRequests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-03-17.
 */
public class Client {
    private String clientId;
    private String serverName;
    private int serverPort;
    private OneKey psk;
    private CoapClient coapClient;

    public Client(String clientId, String serverName, int port, OneKey psk) throws COSE.CoseException
    {
        this.clientId = clientId;
        this.serverName = serverName;
        this.serverPort = port;
        this.psk = psk;
    }

    public Map<String, CBORObject> askForToken(String scope, String audience) throws CoseException, IOException, AceException
    {
        Map<String, CBORObject> params = new HashMap<>();
        params.put("grant_type", Token.clientCredentialsStr);
        params.put("scope", CBORObject.FromObject(scope));
        params.put("aud", CBORObject.FromObject(audience));

        return sendRequest("token", Constants.abbreviate(params), false);
    }

    public Map<String, CBORObject> postToken(CBORObject token) throws CoseException, IOException, AceException
    {
        return sendRequest("authz-info", token, true);
    }

    // Sends a COAP/DTLS request to the server we are configured to connect to.
    public Map<String, CBORObject> sendRequest(String endpointName, CBORObject payload, boolean rsRequest) throws CoseException, IOException, AceException
    {
        String uri = "coaps://" + serverName + ":" + serverPort + "/" + endpointName;

        if(rsRequest){
            // Gets a special client that can send the token as its identity in a request for a RS. Complies with ACE DTLS profile.
            coapClient = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), payload, psk);
            coapClient.setURI(uri);
        }
        else {
            // Sets up a standard COAP/DTLS client.
            coapClient = new CoapClient(uri);
            Connector connector = setupDTLSConnector();
            CoapEndpoint coapEndpoint = new CoapEndpoint(connector, NetworkConfig.getStandard());
            coapClient.setEndpoint(coapEndpoint);
        }

        System.out.println("Sending request to server: " + uri);
        CoapResponse response = coapClient.post(payload.EncodeToBytes(), MediaTypeRegistry.APPLICATION_CBOR);
        Map<String, CBORObject> map = null;
        if(response == null) {
            System.out.println("Server did not respond.");
            return null;
        }

        System.out.println("Response: " + Utils.prettyPrint(response));

        if(response.getCode() != CoAP.ResponseCode.CREATED &&
           response.getCode() != CoAP.ResponseCode.VALID &&
           response.getCode() != CoAP.ResponseCode.DELETED &&
           response.getCode() != CoAP.ResponseCode.CHANGED &&
           response.getCode() != CoAP.ResponseCode.CONTENT)
        {
            System.out.println("Error received in response: " + response.getCode());
            return null;
        }

        CBORObject responseData = null;
        try {
            responseData = CBORObject.DecodeFromBytes(response.getPayload());
        }
        catch(Exception e)
        {
            System.out.println("Reply was received in plain text.");
            return null;
        }

        System.out.println("Response CBOR Payload: " + responseData);
        if(!responseData.getType().equals(CBORType.Map))
        {
            map = new HashMap<>();
            map.put("reply", responseData);
        }
        else {
            map = Constants.unabbreviate(responseData);
        }
        System.out.println(map);

        return map;
    }

    // Creates and starts a DTL connector with this client's ID and AS-PSK.
    private Connector setupDTLSConnector() throws CoseException, IOException
    {
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        builder.setPskStore(new StaticPskStore(clientId, psk.get(KeyKeys.Octet_K).GetByteString()));
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        dtlsConnector.start();
        return dtlsConnector;
    }

    public void stop()
    {
        coapClient.shutdown();
    }
}
