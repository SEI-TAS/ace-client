package edu.cmu.sei.ttg.aaiot.client;

/**
 * Created by Sebastian on 2017-05-11.
 */
public class Program {

    public static void main(String args[])
    {
        try {
            Controller controller = new Controller();
            controller.run();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
