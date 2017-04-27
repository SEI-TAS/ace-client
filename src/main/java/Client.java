import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.as.Token;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebastian on 2017-03-17.
 */
public class Client {
    static byte[] key256 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

    public static void main(String args[])
    {
        try {
            Client.askForToken();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void generateTestKey() throws CoseException
    {
        OneKey akey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        OneKey publicKey = akey.PublicKey();
        String pubStr = Base64.getEncoder().encodeToString(publicKey.EncodeToBytes());
        System.out.println(pubStr);
    }

    private static CoapEndpoint getCoapsEndpoint() throws CoseException, IOException
    {
        //OneKey asymmetricKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        String publicKeyStr = "piJYICg7PY0o/6Wf5ctUBBKnUPqN+jT22mm82mhADWecE0foI1ghAKQ7qn7SL/Jpm6YspJmTWbFG8GWpXE5GAXzSXrialK0pAyYBAiFYIBLW6MTSj4MRClfSUzc8rVLwG8RH5Ak1QfZDs4XhecEQIAE=";
        OneKey publickey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(publicKeyStr)));

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setPskStore(new StaticPskStore("clientA", key256));
        //builder.setIdentity(asymmetricKey.AsPrivateKey(), publickey.AsPublicKey());
        //builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});

        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        dtlsConnector.start();

        CoapEndpoint e = new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard());
        return e;
    }

    public static void askForToken() throws CoseException, IOException, AceException
    {
        CoapClient client = new CoapClient("coaps://localhost/token");
        client.setEndpoint(getCoapsEndpoint());

        Map<String, CBORObject> params = new HashMap<>();
        params.put("grant_type", Token.clientCredentialsStr);
        params.put("scope", CBORObject.FromObject("r_temp rw_config foobar"));
        params.put("aud", CBORObject.FromObject("rs1"));

        CoapResponse response = client.post(Constants.abbreviate(params).EncodeToBytes(),
                                            MediaTypeRegistry.APPLICATION_CBOR);

        if(response != null) {
            CBORObject res = CBORObject.DecodeFromBytes(response.getPayload());
            Map<String, CBORObject> map = Constants.unabbreviate(res);
            System.out.println(map);
        }
        else {
            System.out.println("Server did not respond.");
        }
    }
}
