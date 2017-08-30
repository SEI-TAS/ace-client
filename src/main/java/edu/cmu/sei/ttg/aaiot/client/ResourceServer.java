package edu.cmu.sei.ttg.aaiot.client;

import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.Constants;

import java.util.Map;

/**
 * Created by sebastianecheverria on 8/30/17.
 */
public class ResourceServer
{
    public String ipAddress;
    public int port;
    public boolean isTokenSent = false;
    public CBORObject token = null;
    public String popKeyId = null;
    public OneKey popKey = null;

    public ResourceServer(Map<String, CBORObject> data) throws CoseException
    {
        isTokenSent = false;

        token = data.get("access_token");
        System.out.println("Token :" + token);

        CBORObject popKeyCbor = data.get("cnf");
        System.out.println("Cnf (pop) key: " + popKeyCbor);

        CBORObject popKeyData = popKeyCbor.get(Constants.COSE_KEY_CBOR);
        popKey = new OneKey(popKeyData);
        System.out.println("Cnf (pop) key data: " + popKeyData);

        CBORObject kidCbor = popKeyData.get(KeyKeys.KeyId.AsCBOR());
        popKeyId = new String(kidCbor.GetByteString(), Constants.charset);
        System.out.println("Cnf (pop) key id: " + popKeyId);
    }
}
