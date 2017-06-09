import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.Constants;
import se.sics.ace.client.GetToken;
import se.sics.ace.coap.client.DTLSProfileRequests;

import java.util.Map;

/**
 * Created by Sebastian on 2017-05-11.
 */
public class Program {
    private static byte[] sharedKey128Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    public static void main(String args[])
    {
        try {
            String clientId = "clientA";

            Client asClient = new Client(clientId, "localhost", 5684);
            Map<String, CBORObject> reply = asClient.askForToken("r_temp","rs1");
            if(reply != null) {
                CBORObject token = reply.get("access_token");
                System.out.println("Token :" + token);
                CBORObject popKey = reply.get("cnf");
                System.out.println("Cnf: " + popKey);
                CBORObject keyData = popKey.get(Constants.COSE_KEY_CBOR);
                System.out.println("Cnf key: " + keyData);

                Client rsClient = new Client(clientId, "localhost", 5685);
                rsClient.sendRequest("temp", token, new OneKey(keyData));
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // NOTE: this does not work as it requires RPK, not PSK, which is not what we are using...
    public static void requestsUsingLib()
    {
        String asUri = "coaps://localhost:5684/token";
        String rsUri = "coaps://localhost:5685/authz-info";

        try {
            String clientId = "clientA";
            CBORObject keyData = CBORObject.NewMap();
            keyData.Add(KeyKeys.KeyId.AsCBOR(), clientId.getBytes(Constants.charset));
            keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
            keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(sharedKey128Bytes));
            OneKey sharedKey = new OneKey(keyData);

            CBORObject tokenPayload = GetToken.getClientCredentialsRequest(CBORObject.FromObject("rs1"),
                    CBORObject.FromObject("r_temp"), null);
            //CBORObject token = DTLSProfileRequests.getToken(asUri, tokenPayload, sharedKey);
            //DTLSProfileRequests.postToken(rsUri, token, true);
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
