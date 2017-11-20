package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.Manager;
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
            Manager.getInstance().startRevocationChecker();
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
            Map<String, String> revokedTokens = Manager.getInstance().getRevokedTokens();
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
