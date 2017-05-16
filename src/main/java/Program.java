import com.upokecenter.cbor.CBORObject;

import java.util.Map;

/**
 * Created by Sebastian on 2017-05-11.
 */
public class Program {
    public static void main(String args[])
    {
        try {
            Client client = new Client();
            Map<String, CBORObject> reply = client.askForToken("localhost:5684");
            CBORObject token = reply.get("access_token");
            System.out.println(token);
            client.askForResource("localhost:5685", token);
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
