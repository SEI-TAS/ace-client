package edu.cmu.sei.ttg.aaiot.client.commandline;

import edu.cmu.sei.ttg.aaiot.client.Manager;

import java.util.Scanner;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class CommandLineUI
{
    public void run()
    {
        Manager manager;
        try
        {
            manager = Manager.getInstance();
        }
        catch(Exception e)
        {
            System.out.println("Error starting up: " + e.toString());
            e.printStackTrace();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while(true)
        {
            try
            {
                System.out.println("");
                System.out.println("Choose (p)air, (t)oken request, (r)esource request, (v) start/stop revoked tokens check, or (q)uit: ");
                String choiceString = scanner.nextLine();
                if(choiceString == null || choiceString.equals(""))
                {
                    System.out.println("No command selected, try again.");
                    continue;
                }

                char choice = choiceString.charAt(0);
                switch (choice)
                {
                    case 'p':
                        boolean success = manager.enableAndWaitForPairing();
                        if (success)
                        {
                            System.out.println("Finished pairing process!");
                        }
                        else
                        {
                            System.out.println("Pairing aborted.");
                        }
                        break;
                    case 't':
                        System.out.println("Input the resource audience to request a token for (RS name): ");
                        String rsId = scanner.nextLine();
                        System.out.println("Input the resource scope(s) to request a token for, separated by space: ");
                        String scopes = scanner.nextLine();
                        manager.requestToken(rsId, scopes);
                        break;
                    case 'r':
                        System.out.println("Input the resource audience to send the request to (RS name): ");
                        String rsName = scanner.nextLine();

                        System.out.println("Input resource server's IP, or (Enter) to use default (" + Manager.DEFAULT_RS_IP + "): ");
                        String rsIP = scanner.nextLine();
                        if (rsIP.equals(""))
                        {
                            rsIP = Manager.DEFAULT_RS_IP;
                        }
                        System.out.println("Input resource server's port, or (Enter) to use default (" + Manager.DEFAULT_RS_PORT + "): ");
                        String rsPort = scanner.nextLine();
                        int rsPortInt = Manager.DEFAULT_RS_PORT;
                        if (!rsPort.equals(""))
                        {
                            rsPortInt = Integer.parseInt(rsPort);
                        }

                        System.out.println("Input the resource name: ");
                        String resourceName = scanner.nextLine();
                        manager.requestResource(rsName, rsIP, rsPortInt, resourceName);
                        break;
                    case 'v':
                        manager.toggleRevocationChecker();
                        break;
                    case 'q':
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid command.");
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error processing command: " + ex.toString());
                ex.printStackTrace();
            }
        }
    }
}
