package edu.cmu.sei.ttg.aaiot.client.gui.controllers;

import edu.cmu.sei.ttg.aaiot.client.Manager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Created by sebastianecheverria on 11/20/17.
 */
public class ResourcesController
{
    @FXML private TextField deviceIdTextField;
    @FXML private TextField resourceTextField;
    @FXML private TextField deviceIpTextField;
    @FXML private TextField devicePortTextField;
    @FXML private TextArea resultsTextArea;

    /**
     * Sets up default values.
     */
    public void initialize()
    {
        // Default IP and port.
        deviceIpTextField.setText(Manager.DEFAULT_RS_IP);
        devicePortTextField.setText(String.valueOf(Manager.DEFAULT_RS_PORT));
    }

    /**
     * Sends a resource request.
     */
    public void requestResource()
    {
        try
        {
            int port = Integer.parseInt(devicePortTextField.getText());
            String result = Manager.getInstance().requestResource(deviceIdTextField.getText(), deviceIpTextField.getText(), port,
                            resourceTextField.getText());
            resultsTextArea.setText(result);
        }
        catch (Exception e)
        {
            System.out.println("Error requesting resource: " + e.toString());
            new Alert(Alert.AlertType.ERROR, "Error requesting resource: " + e.getMessage()).showAndWait();
        }
    }
}
