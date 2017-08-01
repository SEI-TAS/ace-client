package edu.cmu.sei.ttg.aaiot.client;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.client.cc2531.CC2531Controller;
import edu.cmu.sei.ttg.aaiot.client.pairing.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.client.pairing.PairingManager;
import se.sics.ace.AceException;
import se.sics.ace.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller implements ICredentialStore {

    private static final int AS_PORT = 5684;
    private static final int RS_PORT = 5685;

    private static final String AS_IP = "localhost";
    private static final String RS_IP = "localhost";

    private static final String clientId = "clientA";

    private String asId = null;
    private OneKey asPSK = null;

    private CBORObject token = null;
    private String kid = null;
    private OneKey rsPSK = null;

    private boolean tokenSent = false;

    public void run() throws COSE.CoseException, IOException, AceException, javax.usb.UsbException
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
                case 'u':
                    System.out.println("USB test");
                    CC2531Controller cc2531 = new CC2531Controller();
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        }

    }

    public void pair() throws IOException
    {
        //byte[] AS256BytesPSK = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
        //asPSK = createOneKeyFromBytes(AS256BytesPSK);
        PairingManager pairingManager = new PairingManager(this);
        pairingManager.startPairing();
    }

    public void requestToken(String rsName, String rsScope) throws COSE.CoseException, IOException, AceException
    {
        if(asPSK == null)
        {
            System.out.println("edu.cmu.sei.ttg.aaiot.client.Client not paired yet.");
            return;
        }

        Client asClient = new Client(clientId, AS_IP, AS_PORT, asPSK, null, null, tokenSent);
        Map<String, CBORObject> reply = asClient.getAccessToken(rsScope, rsName);
        if(reply != null) {
            token = reply.get("access_token");
            System.out.println("Token :" + token);
            tokenSent = false;

            CBORObject popKey = reply.get("cnf");
            System.out.println("Cnf: " + popKey);

            CBORObject rsKeyData = popKey.get(Constants.COSE_KEY_CBOR);
            System.out.println("Cnf key: " + rsKeyData);

            CBORObject kidCbor = rsKeyData.get(KeyKeys.KeyId.AsCBOR());
            kid = new String(kidCbor.GetByteString(), Constants.charset);
            System.out.println("Cnf key id: " + kid);

            rsPSK = new OneKey(rsKeyData);
        }
        asClient.stop();
    }

    public void requestResource(String rsResource) throws COSE.CoseException, IOException, AceException
    {
        if(rsPSK == null || token == null) {
            System.out.println("Token and POP not obtained yet.");
            return;
        }

        Client rsClient = new Client(clientId, RS_IP, RS_PORT, rsPSK, token, kid, tokenSent);
        rsClient.sendRequest(rsResource, "get", null);
        tokenSent = true;
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

    @Override
    public String getId()
    {
        return clientId;
    }

    @Override
    public boolean storeAS(String asId, byte[] psk)
    {
        try
        {
            this.asId = asId;
            this.asPSK = createOneKeyFromBytes(psk);
            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error storing AS key: " + ex.toString());
            return false;
        }
    }

}
