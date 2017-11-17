package edu.cmu.sei.ttg.aaiot.client.commandline;

/**
 * Created by Sebastian on 2017-05-11.
 */
public class CommandLineApplication
{

    public static void main(String args[])
    {
        try
        {
            CommandLineUI ui = new CommandLineUI();
            ui.run();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
