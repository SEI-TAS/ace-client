package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.Manager;
import edu.cmu.sei.ttg.aaiot.client.gui.models.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.threads.TaskThread;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

import java.net.InetAddress;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class ASListController
{
    @FXML private TableView<AuthorizationServer> asTableView;
    @FXML private Button pairingButton;

    /**
     * Sets up the view.
     */
    @FXML
    public void initialize()
    {
        try
        {
            fillASTable();
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
    private void fillASTable() throws Exception
    {
        ObservableList<AuthorizationServer> tableData = asTableView.getItems();
        tableData.clear();
        String ASId = Manager.getInstance().getCredentialStore().getASid();
        InetAddress ipAddress = Manager.getInstance().getCredentialStore().getASIP();
        if(ipAddress != null)
        {
            tableData.add(new AuthorizationServer(ASId, ipAddress.getHostAddress()));
        }
    }

    /**
     * Enables the pairing server to receive a request, and blocks waiting for it.
     */
    public void enablePairing()
    {
        new TaskThread(() -> startPairingThread()).start();
    }

    /**
     * Actual thread for pairing, to avoid blocking GUI thread.
     */
    private void startPairingThread()
    {
        try
        {
            pairingButton.setDisable(true);
            Manager.getInstance().enableAndWaitForPairing();
            fillASTable();
        }
        catch (Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error during pairing: " + e.toString()).showAndWait());
            e.printStackTrace();
        }
        finally
        {
            // Ensure that in either case, the button is re-enabled.
            pairingButton.setDisable(false);
        }
    }
}
