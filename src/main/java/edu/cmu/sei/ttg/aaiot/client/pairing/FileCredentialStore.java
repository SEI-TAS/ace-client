package edu.cmu.sei.ttg.aaiot.client.pairing;

import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 8/11/17.
 */
public class FileCredentialStore implements ICredentialStore
{
    private String asId = null;
    private OneKey asPSK = null;

    private String filePath;

    public FileCredentialStore(String filePath) throws IOException, CoseException
    {
        this.filePath = filePath;

        FileInputStream fs;
        try {
            fs = new FileInputStream(filePath);
        }
        catch(IOException ex)
        {
            System.out.println("File Store file " + filePath + " not found, will be created.");
            return;
        }

        File file = new File(filePath, filePath);
        System.out.println("length " + file.length());
        byte[] data = new byte[10000];
        fs.read(data);
        fs.close();
        String jsonString = new String(data, "UTF-8");
        System.out.println(jsonString);

        JSONObject json = new JSONObject(jsonString);

        this.asId = json.getString("AS_ID");
        this.asPSK = createOneKeyFromBytes(Base64.getDecoder().decode(json.getString("AS_PSK")));
    }

    @Override
    public boolean storeAS(String asId, byte[] psk)
    {
        try
        {
            this.asId = asId;
            this.asPSK = createOneKeyFromBytes(psk);

            JSONObject json = new JSONObject();
            json.put("AS_ID", asId);
            json.put("AS_PSK", Base64.getEncoder().encode(psk));

            FileWriter file = new FileWriter(filePath, false);
            file.write(json.toString());
            file.flush();
            file.close();

            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error storing AS key: " + ex.toString());
            return false;
        }
    }

    // Creates a OneKey from raw key data.
    public OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        return new OneKey(keyData);
    }

    @Override
    public String getASid()
    {
        return asId;
    }

    @Override
    public OneKey getASPSK()
    {
        return asPSK;
    }
}
