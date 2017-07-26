package edu.cmu.sei.ttg.aaiot.client.pairing;

/**
 * Created by sebastianecheverria on 7/25/17.
 */
public interface ICredentialStore
{
    String getId();
    boolean storeAS(String id, byte[] psk);
}
