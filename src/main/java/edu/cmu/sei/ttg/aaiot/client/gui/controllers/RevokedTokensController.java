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

package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.AceClient;
import edu.cmu.sei.ttg.aaiot.client.gui.models.Token;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

import java.util.Map;

/**
 * Created by sebastianecheverria on 11/17/17.
 */
public class RevokedTokensController
{
    @FXML
    private TableView<Token> revokedTokensTableView;

    /**
     * Sets up the view.
     */
    @FXML
    public void initialize()
    {
        try
        {
            AceClient.getInstance().startRevocationChecker();
            fillTable();
        }
        catch (Exception e)
        {
            System.out.println("Error setting up view: " + e.toString());
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error setting up view: " + e.toString()).showAndWait();
        }
    }

    /**
     * Updates the data in the table with the current data.
     * @throws Exception
     */
    public void fillTable()
    {
        try
        {
            ObservableList<Token> tokensTableData = revokedTokensTableView.getItems();
            tokensTableData.clear();
            Map<String, String> revokedTokens = AceClient.getInstance().getRevokedTokens();
            if (revokedTokens != null)
            {
                for (String tokenId : revokedTokens.keySet())
                {
                    String rsId = revokedTokens.get(tokenId);
                    tokensTableData.add(new Token(tokenId, rsId));
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("Error updating revoked token data: " + e.toString());
        }
    }

}
