import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.Constants;
import se.sics.ace.client.GetToken;
import se.sics.ace.coap.client.DTLSProfileRequests;

import java.util.Base64;
import java.util.Map;

/**
 * Created by Sebastian on 2017-05-11.
 */
public class Program {

    public static void main(String args[])
    {
        try {
            String clientId = "clientA";

            // This should be obtained as the result of pairing.
            byte[] AS256BytesPSK = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

            Client asClient = new Client(clientId, "localhost", 5684, AS256BytesPSK);
            Map<String, CBORObject> reply = asClient.askForToken("r_temp","rs1");
            if(reply != null) {
                CBORObject token = reply.get("access_token");
                System.out.println("Token :" + token);
                CBORObject popKey = reply.get("cnf");
                System.out.println("Cnf: " + popKey);
                CBORObject rsKeyData = popKey.get(Constants.COSE_KEY_CBOR);
                System.out.println("Cnf key: " + rsKeyData);

                Client rsClient = new Client(clientId, "localhost", 5685, new OneKey(rsKeyData));
                rsClient.sendRequest("temp", token, true);
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

//
//    // NOTE: this does not work as it requires RPK, not PSK, which is not what we are using...
//    public static void requestsUsingLib()
//    {
//        byte[] sharedKey128Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
//
//        String asUri = "coaps://localhost:5684/token";
//        String rsUri = "coaps://localhost:5685/authz-info";
//
//        try {
//            String clientId = "clientA";
//            CBORObject keyData = CBORObject.NewMap();
//            keyData.Add(KeyKeys.KeyId.AsCBOR(), clientId.getBytes(Constants.charset));
//            keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
//            keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(sharedKey128Bytes));
//            OneKey sharedKey = new OneKey(keyData);
//
//            CBORObject tokenPayload = GetToken.getClientCredentialsRequest(CBORObject.FromObject("rs1"),
//                    CBORObject.FromObject("r_temp"), null);
//            //CBORObject token = DTLSProfileRequests.getToken(asUri, tokenPayload, sharedKey);
//            //DTLSProfileRequests.postToken(rsUri, token, true);
//        } catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    // NOTE: not used, only for RPK.
//    public static void generateTestKey() throws CoseException
//    {
//        //OneKey asymmetricKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
//        //String publicKeyStr = "piJYICg7PY0o/6Wf5ctUBBKnUPqN+jT22mm82mhADWecE0foI1ghAKQ7qn7SL/Jpm6YspJmTWbFG8GWpXE5GAXzSXrialK0pAyYBAiFYIBLW6MTSj4MRClfSUzc8rVLwG8RH5Ak1QfZDs4XhecEQIAE=";
//        //OneKey publickey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(publicKeyStr)));
//        //builder.setIdentity(asymmetricKey.AsPrivateKey(), publickey.AsPublicKey());
//        //builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
//
//        OneKey akey = OneKey.generateKey(AlgorithmID.ECDSA_256);
//        OneKey publicKey = akey.PublicKey();
//        String pubStr = Base64.getEncoder().encodeToString(publicKey.EncodeToBytes());
//        System.out.println(pubStr);
//    }

}
