package edu.cmu.sei.ttg.aaiot.client.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class Token
{
    private final SimpleStringProperty id = new SimpleStringProperty("");
    private final SimpleStringProperty rsId = new SimpleStringProperty("");

    public Token(String id, String rsId)
    {
        setId(id);
        setRsId(rsId);
    }

    public void setId(String id)
    {
        this.id.set(id);
    }

    public void setRsId(String rsId)
    {
        this.rsId.set(rsId);
    }

    public String getId()
    {
        return id.get();
    }

    public String getRsId()
    {
        return rsId.get();
    }
}
