package edu.cmu.sei.ttg.aaiot.client.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class AuthorizationServer
{
    private final SimpleStringProperty id = new SimpleStringProperty("");
    private final SimpleStringProperty ipAddress = new SimpleStringProperty("");

    public AuthorizationServer(String id, String ipAddress)
    {
        setId(id);
        setIpAddress(ipAddress);
    }

    public void setId(String id)
    {
        this.id.set(id);
    }

    public String getId()
    {
        return id.get();
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress.set(ipAddress);
    }

    public String getIpAddress()
    {
        return ipAddress.get();
    }

}
