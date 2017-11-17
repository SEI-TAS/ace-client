package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.Manager;
import edu.cmu.sei.ttg.aaiot.client.gui.models.Token;
import edu.cmu.sei.ttg.aaiot.tokens.ResourceServer;
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

    /**
     * Sets up the view.
     */
    @FXML
    public void initialize()
    {
        try
        {
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
        Map<String, ResourceServer> tokens = Manager.getInstance().getTokenStore().getTokens();
        if(tokens != null)
        {
            for (String rsId : tokens.keySet())
            {
                ResourceServer resourceServer = tokens.get(rsId);
                String tokenKeyIdString = resourceServer.getPopKeyIdAsString();
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

        try
        {
            Manager.getInstance().requestToken(deviceId, scopes);
            fillTable();
        }
        catch(NoPermissionException e)
        {
            System.out.println(e.toString());
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        }
        catch(Exception e)
        {
            System.out.println("Error requesting token: " + e.toString());
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error requesting token: " + e.toString()).showAndWait();
        }
    }
}