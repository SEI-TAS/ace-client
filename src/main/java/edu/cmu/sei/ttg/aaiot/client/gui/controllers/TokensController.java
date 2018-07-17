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
import edu.cmu.sei.ttg.aaiot.network.CoapException;
import edu.cmu.sei.ttg.aaiot.tokens.TokenInfo;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javax.naming.NoPermissionException;
import java.util.Map;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class TokensController
{
    @FXML private TableView<Token> tokensTableView;
    @FXML private TextField deviceIdTextField;
    @FXML private TextField scopesTextField;
    @FXML private TextField asIpTextField;
    @FXML private TextField asCoapsPortTextField;

    /**
     * Sets up the view.
     */
    @FXML
    public void initialize()
    {
        try
        {
            // Default IP and port.
            asIpTextField.setText(AceClient.getInstance().getCredentialStore().getASIP().getHostAddress());
            asCoapsPortTextField.setText(String.valueOf(AceClient.DEFAULT_AS_COAPS_PORT));

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
     * Updates the data in the table with the current data in the DB.
     * @throws Exception
     */
    public void fillTable() throws Exception
    {
        ObservableList<Token> tokensTableData = tokensTableView.getItems();
        tokensTableData.clear();
        Map<String, TokenInfo> tokens = AceClient.getInstance().getTokenStore().getTokens();
        if(tokens != null)
        {
            for (String rsId : tokens.keySet())
            {
                TokenInfo tokenInfo = tokens.get(rsId);
                String tokenKeyIdString = tokenInfo.getTokenId();
                tokensTableData.add(new Token(tokenKeyIdString, rsId));
            }
        }
    }

    /**
     * Executing the request token button action.
     */
    public void requestToken()
    {
        String deviceId = deviceIdTextField.getText();
        String scopes = scopesTextField.getText();
        String asIp = asIpTextField.getText();
        int port = Integer.parseInt(asCoapsPortTextField.getText());

        try
        {
            boolean success = AceClient.getInstance().requestToken(deviceId, scopes, asIp, port);
            if(success)
            {
                new Alert(Alert.AlertType.INFORMATION, "New token obtained!").showAndWait();
                fillTable();
            }
            else
            {
                new Alert(Alert.AlertType.WARNING, "Could not obtain token.").showAndWait();
            }
        }
        catch(NoPermissionException e)
        {
            System.out.println(e.toString());
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        }
        catch(CoapException ex)
        {
            String errorMessage = "Token could not be obtained; server reported error " + ex.getErrorCode() + ": " +
                    ex.getErrorName() + ". " + ex.getErrorDescription();
            System.out.println(errorMessage);
            new Alert(Alert.AlertType.WARNING, errorMessage).showAndWait();
        }
        catch(Exception e)
        {
            System.out.println("Error requesting token: " + e.toString());
            //e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error requesting token: " + e.toString()).showAndWait();
        }
    }
}
