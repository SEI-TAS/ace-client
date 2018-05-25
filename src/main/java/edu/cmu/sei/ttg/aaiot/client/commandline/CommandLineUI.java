/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.client.commandline;

import edu.cmu.sei.ttg.aaiot.client.AceClient;

import java.util.Scanner;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class CommandLineUI
{
    public void run()
    {
        AceClient manager;
        try
        {
            manager = AceClient.getInstance();
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

                        System.out.println("Input resource server's IP, or (Enter) to use default (" + AceClient.DEFAULT_RS_IP + "): ");
                        String rsIP = scanner.nextLine();
                        if (rsIP.equals(""))
                        {
                            rsIP = AceClient.DEFAULT_RS_IP;
                        }
                        System.out.println("Input resource server's port, or (Enter) to use default (" + AceClient.DEFAULT_RS_COAPS_PORT + "): ");
                        String rsPort = scanner.nextLine();
                        int rsPortInt = AceClient.DEFAULT_RS_COAPS_PORT;
                        if (!rsPort.equals(""))
                        {
                            rsPortInt = Integer.parseInt(rsPort);
                        }

                        System.out.println("Input the resource name: ");
                        String resourceName = scanner.nextLine();
                        manager.requestResource(rsName, rsIP, rsPortInt, AceClient.DEFAULT_RS_COAP_PORT, resourceName);
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
