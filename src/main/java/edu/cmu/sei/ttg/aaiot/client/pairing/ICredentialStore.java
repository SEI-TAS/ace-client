package edu.cmu.sei.ttg.aaiot.client.pairing;

import COSE.OneKey;

/**
 * Created by sebastianecheverria on 7/25/17.
 */
public interface ICredentialStore
{
    boolean storeAS(String id, byte[] psk);
    String getASid();
    OneKey getASPSK();
}
