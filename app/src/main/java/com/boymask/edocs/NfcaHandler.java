package com.boymask.edocs;

import android.nfc.tech.IsoDep;

public class NfcaHandler {
    private IsoDep desfire;
    private final byte[] NATIVE_SELECT_APP_COMMAND = new byte[]
            {
                    (byte) 0x90, (byte) 0x5A, (byte) 0x00, (byte) 0x00, 3,  // SELECT
                    (byte) 0x5F, (byte) 0x84, (byte) 0x15, (byte) 0x00      // APPLICATION ID
            };
    private final byte[] NATIVE_SELECT_FILE_COMMAND = new byte[]
            {
                    (byte) 0x90, (byte) 0xBD, (byte) 0x00, (byte) 0x00, 7,  // READ
                    (byte) 0x01,                                            // FILE ID
                    (byte) 0x00, (byte) 0x00, (byte) 0x00,                  // OFFSET
                    (byte) 0x00, (byte) 0x00, (byte) 0x00,                  // LENGTH
                    (byte) 0x00
            };

}
