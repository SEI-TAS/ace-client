package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.Manager;
import edu.cmu.sei.ttg.aaiot.client.gui.models.Token;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

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
    public void addRevokedToken(Token token)
    {
        ObservableList<Token> tokensTableData = revokedTokensTableView.getItems();
        tokensTableData.add(token);
    }
}
