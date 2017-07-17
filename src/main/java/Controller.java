import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.AceException;
import se.sics.ace.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller {

    private static final String AS_IP = "localhost";
    private static final int AS_PORT = 5684;
    private static final String RS_IP = "localhost";
    private static final int RS_PORT = 5685;

    private String clientId = null;
    private OneKey asPSK = null;

    private CBORObject token = null;
    private OneKey rsPSK = null;

    public void run() throws COSE.CoseException, IOException, AceException
    {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (p)air, (t)oken request, (r)esource request, or (q)uit: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'p':
                    pair();
                    System.out.println("Paired!");
                    break;
                case 't':
                    requestToken("rs1", "r_temp");
                    break;
                case 'r':
                    requestResource("temp");
                    break;
                case 'q':
                    System.exit(0);
                default:
                    System.out.println("Invalid command.");
            }
        }

    }

    public void pair() throws COSE.CoseException
    {
        clientId = "clientA";
        byte[] AS256BytesPSK = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
        asPSK = createOneKeyFromBytes(AS256BytesPSK);
    }

    public void requestToken(String rsName, String rsScope) throws COSE.CoseException, IOException, AceException
    {
        if(clientId == null || asPSK == null)
        {
            System.out.println("Client not paired yet.");
            return;
        }

        Client asClient = new Client(clientId, AS_IP, AS_PORT, asPSK, null);
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScope, rsName);
        if(reply != null) {
            asClient.stop();
            token = reply.get("access_token");
            System.out.println("Token :" + token);

            CBORObject popKey = reply.get("cnf");
            System.out.println("Cnf: " + popKey);

            CBORObject rsKeyData = popKey.get(Constants.COSE_KEY_CBOR);
            System.out.println("Cnf key: " + rsKeyData);

            rsPSK = new OneKey(rsKeyData);
        }
    }

    public void requestResource(String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(rsPSK == null || token == null) {
            System.out.println("Token and POP not obtained yet.");
            return;
        }

        Client rsClient = new Client(clientId, RS_IP, RS_PORT, rsPSK, token);
        rsClient.sendRequest(rsResource, "get", null);
        rsClient.stop();
    }

    // Creates a OneKey from raw key data.
    public OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        OneKey key = new OneKey(keyData);
        return key;
    }

}
