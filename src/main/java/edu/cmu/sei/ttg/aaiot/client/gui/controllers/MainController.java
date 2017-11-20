package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class MainController
{
    @FXML private TabPane tabPane;
    @FXML private Tab tokensTab;
    @FXML private Tab revokedTokensTab;

    @FXML private TokensController tokensTabPageController;
    @FXML private RevokedTokensController revokedTokensTabPageController;

    @FXML
    public void initialize()
    {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) ->
        {
            if (newTab == tokensTab)
            {
                try
                {
                    tokensTabPageController.fillTable();
                }
                catch(Exception e)
                {
                    System.out.println("Error updating tokens: " + e.toString());
                }
            }
            if (newTab == revokedTokensTab)
            {
                try
                {
                    revokedTokensTabPageController.fillTable();
                }
                catch(Exception e)
                {
                    System.out.println("Error updating revoked tokens: " + e.toString());
                }
            }

        });
    }
}
