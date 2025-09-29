package com.isw.payapp.devices.dspread.utils;

public class Poskeys {

    protected String RSA_public_key = "E9D221BF4C7D56C562AF4E12E17EC31D73F7BD61EFCFCBF4D9E2C1C779CBB75873CFA4BFF0488F56E120B8493D8EE7E0DEBF420AEAC00F58112686DD3F62E00A9DCF74A78D4984E3951619C9C9442C7E60F2E51275EA913C66ADE78A3514C1B3F8F5E844C11B8E687F15E839203ED1CB315CB8AF61EE810DA6D1ED5C09413743";

    public enum RSA_KEY_LEN {
        RSA_KEY_1024,
        RSA_KEY_2048
    }

    public String getRSA_public_key() {
        return this.RSA_public_key;
    }

    public void setRSA_public_key(String str) {
        this.RSA_public_key = str;
    }
}
