/**
 * Created by Sebastian on 2017-05-11.
 */
public class Program {
    public static void main(String args[])
    {
        try {
            Client client = new Client();
            client.askForToken();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
