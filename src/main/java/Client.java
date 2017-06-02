import COSE.AlgorithmID;
import COSE.CoseException;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-03-17.
 */
public class Client {
    static byte[] sharedKey256Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

    private String clientId;
    private String serverName;
    private int serverPort;

    public Client(String clientId, String serverName, int port)
    {
        this.clientId = clientId;
        this.serverName = serverName;
        this.serverPort = port;
    }

    private Connector setupDTLSConnector() throws CoseException, IOException
    {
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        builder.setPskStore(new StaticPskStore(clientId, sharedKey256Bytes));
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        dtlsConnector.start();
        return dtlsConnector;
    }

    public Map<String, CBORObject> askForToken() throws CoseException, IOException, AceException
    {
        Map<String, CBORObject> params = new HashMap<>();
        params.put("grant_type", Token.clientCredentialsStr);
        params.put("scope", CBORObject.FromObject("r_temp"));
        params.put("aud", CBORObject.FromObject("rs1"));

        return sendRequest("token", Constants.abbreviate(params), null);
    }

    public Map<String, CBORObject> postToken(CBORObject token, OneKey psk) throws CoseException, IOException, AceException
    {
        // CWT token has been encoded with the "shared by all" PSK we are using to test.
        return sendRequest("authz-info", token, psk);
    }

    public Map<String, CBORObject> sendRequest(String endpointName, CBORObject payload, OneKey psk) throws CoseException, IOException, AceException
    {
        String uri = "coaps://" + serverName + ":" + serverPort + "/" + endpointName;

        CoapClient client;
        if(psk != null){
            client = DTLSProfileRequests.getPskClient(new InetSocketAddress(serverName, serverPort), payload, psk);
            client.setURI(uri);
        }
        else {
            client = new CoapClient(uri);
            Connector connector = setupDTLSConnector();
            CoapEndpoint coapEndpoint = new CoapEndpoint(connector, NetworkConfig.getStandard());
            client.setEndpoint(coapEndpoint);
        }

        System.out.println("Sending request to server: " + uri);
        CoapResponse response = client.post(payload.EncodeToBytes(),
                MediaTypeRegistry.APPLICATION_CBOR);
        Map<String, CBORObject> map = null;
        if(response != null) {
            System.out.println("Response: " + Utils.prettyPrint(response));

            if(response.getCode() != CoAP.ResponseCode.CREATED)
            {
                System.out.println("Error received in response: " + response.getCode());
                return null;
            }

            CBORObject res = CBORObject.DecodeFromBytes(response.getPayload());
            System.out.println("Response Payload: " + res);
            if(!res.getType().equals(CBORType.Map))
            {
                map = new HashMap<>();
                map.put("reply", res);
            }
            else {
                map = Constants.unabbreviate(res);
            }
            System.out.println(map);
        }
        else {
            System.out.println("Server did not respond.");
        }

        return map;
    }

    public static void generateTestKey() throws CoseException
    {
        //OneKey asymmetricKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        //String publicKeyStr = "piJYICg7PY0o/6Wf5ctUBBKnUPqN+jT22mm82mhADWecE0foI1ghAKQ7qn7SL/Jpm6YspJmTWbFG8GWpXE5GAXzSXrialK0pAyYBAiFYIBLW6MTSj4MRClfSUzc8rVLwG8RH5Ak1QfZDs4XhecEQIAE=";
        //OneKey publickey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(publicKeyStr)));
        //builder.setIdentity(asymmetricKey.AsPrivateKey(), publickey.AsPublicKey());
        //builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});

        OneKey akey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        OneKey publicKey = akey.PublicKey();
        String pubStr = Base64.getEncoder().encodeToString(publicKey.EncodeToBytes());
        System.out.println(pubStr);
    }
}
