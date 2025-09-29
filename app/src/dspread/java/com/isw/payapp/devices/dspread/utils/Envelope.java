package com.isw.payapp.devices.dspread.utils;

import com.dspread.xpos.Util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Envelope {
    public static String digitalEnvelopStr;


    public Envelope() {

    }

    public byte[] readMessageFile(String fileName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream stream = null;
        byte[] results = new byte[0];

        try {
            stream = new FileInputStream(fileName);
            boolean var5 = false;

            int c;
            while ((c = stream.read()) != -1) {
                baos.write(c);
            }

            results = baos.toByteArray();
        } catch (Exception var18) {
            var18.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException var17) {

                }
            }

            try {
                baos.close();
            } catch (IOException var16) {

            }

        }

        return results;
    }

    public static byte[] packageMessage(byte[] message) {
        byte[] results = new byte[message.length + 8];

        for (int i = 0; i < results.length; ++i) {
            results[i] = 0;
        }

        byte[] lenBytes = Utils.int2Byte(message.length);
        System.arraycopy(lenBytes, 0, results, 0, lenBytes.length);
        System.arraycopy(message, 0, results, 8, message.length);
        System.out.println("results: "+bytes2hex(results));
        return results;
    }

    public static byte[] getTdesKey() {
        return new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    }

    public static String bytes2hex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src != null && src.length > 0) {
            for (int i = 0; i < src.length; ++i) {
                int v = src[i] & 255;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }

                stringBuilder.append(hv);
            }

            return stringBuilder.toString().toUpperCase();
        } else {
            return null;
        }
    }

    public static byte[] byteEvelope(byte[] message, RSA senderRsa, RSA receiverRsa) throws Exception {
        return byteEvelope(message, senderRsa, receiverRsa, 1024);
    }

    public static byte[] byteEvelope(byte[] message, RSA senderRsa, RSA receiverRsa, int RSA_len) throws Exception {
        byte[] encrypedTdesKey = receiverRsa.encrypt(getTdesKey());
        byte[] encrypedMessage = encrypt(message, senderRsa);
        byte[] toSha1Message = new byte[encrypedTdesKey.length + encrypedMessage.length];
        System.arraycopy(encrypedTdesKey, 0, toSha1Message, 0, encrypedTdesKey.length);
        System.arraycopy(encrypedMessage, 0, toSha1Message, encrypedTdesKey.length, encrypedMessage.length);
        byte[] signedMessage = senderRsa.sign(toSha1Message);
        byte[] results = new byte[4 + encrypedTdesKey.length + encrypedMessage.length + signedMessage.length];
        int len = encrypedTdesKey.length + encrypedMessage.length + signedMessage.length;
        byte[] lenBytes = Utils.int2Byte(len);
        if (RSA_len == 2048) {
            lenBytes[3] = (byte) 0x80;
        }
        System.out.println("encrypedTdesKey:" + encrypedTdesKey.length + "\n" + "encrypedMessage:" + encrypedMessage.length + "\n" + "signedMessage:" + signedMessage.length);
        System.arraycopy(lenBytes, 0, results, 0, lenBytes.length);
        System.arraycopy(encrypedTdesKey, 0, results, lenBytes.length, encrypedTdesKey.length);
        System.arraycopy(encrypedMessage, 0, results, lenBytes.length + encrypedTdesKey.length, encrypedMessage.length);
        System.arraycopy(signedMessage, 0, results, lenBytes.length + encrypedTdesKey.length + encrypedMessage.length, signedMessage.length);
        return results;
    }

    public static byte[] byteTokenEvelope(byte[] message, RSA senderRsa, RSA receiverRsa, int RSA_len) throws Exception {
        byte[] encrypedMessage = receiverRsa.encrypt(packageMessage(message));
        byte[] toSha1Message = new byte[encrypedMessage.length];
//        System.arraycopy(encrypedTdesKey, 0, toSha1Message, 0, encrypedTdesKey.length);
        System.arraycopy(encrypedMessage, 0, toSha1Message, 0, encrypedMessage.length);
        byte[] signedMessage = senderRsa.sign(toSha1Message);
        byte[] results = new byte[4 +  encrypedMessage.length + signedMessage.length];
        int len = encrypedMessage.length + signedMessage.length;
        byte[] lenBytes = Utils.int2Byte(len);
        if (RSA_len == 2048) {
            lenBytes[3] = (byte) 0x81;
        }
        System.out.println("encrypedTdesKey:"  + "\n" + "encrypedMessage:" + encrypedMessage.length + "\n" + "signedMessage:" + signedMessage.length);
        System.arraycopy(lenBytes, 0, results, 0, lenBytes.length);
        System.arraycopy(encrypedMessage, 0, results, lenBytes.length , encrypedMessage.length);
        System.arraycopy(signedMessage, 0, results, lenBytes.length  + encrypedMessage.length, signedMessage.length);
        return results;
    }

    public static byte[] encrypt(byte[] message, RSA senderRsa) throws Exception {
        byte[] packagedMessage = packageMessage(message);
        int blockSize = packagedMessage.length / 8;
        if (packagedMessage.length % 8 != 0) {
            ++blockSize;
        }
        byte[] padedPackagedMessage = new byte[blockSize * 8];
        int i;
        for (i = 0; i < padedPackagedMessage.length; ++i) {
            padedPackagedMessage[i] = -1;
        }

        System.arraycopy(packagedMessage, 0, padedPackagedMessage, 0, packagedMessage.length);
        byte[] encryptedMess = new byte[blockSize * 8];

        for (i = 0; i < blockSize; ++i) {
            byte[] temp = new byte[8];
            byte[] temp2 = new byte[8];
            System.arraycopy(padedPackagedMessage, i * 8, temp, 0, 8);
            temp2 = TDES.tdesCBCEncypt(getTdesKey(), temp);
            System.arraycopy(temp2, 0, encryptedMess, i * 8, 8);
        }

        return encryptedMess;
    }

    public byte[] sha1(byte[] message) {
        byte[] results = new byte[0];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(message);
            results = md.digest();
            System.out.println("PlainText message:" + bytesToHexString(message));
            System.out.println("sha-1:" + bytesToHexString(results));
        } catch (NoSuchAlgorithmException var4) {

        }
        return results;
    }

    public byte[] sha1(String message) {
        return this.sha1(message.getBytes());
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src != null && src.length > 0) {
            for (int i = 0; i < src.length; ++i) {
                int v = src[i] & 255;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }

                stringBuilder.append(hv);
            }

            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString != null && !hexString.equals("")) {
            hexString = hexString.toUpperCase();
            int length = hexString.length() / 2;
            char[] hexChars = hexString.toCharArray();
            byte[] d = new byte[length];

            for (int i = 0; i < length; ++i) {
                int pos = i * 2;
                d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
            }

            return d;
        } else {
            return null;
        }
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static void main(String[] args) {
//        String aString = getDigitalEnvelopStr();
//        System.out.println("-------->" + aString);
        Envelope envelope = new Envelope();
//        String s = "A0000005246DB747E8CB3615E8D26231355488F3C76C4746F7BB1C381E6C6E6ABF0A6D7CD93CFC6B2C310288CA8BE7EE1730DE621A59D1BB2D8C02C9148FA06E5D1F5E672EEFCE8AECBAD4A1C18F3175F1BEA1AEF539376592366B46A5044E32E59B3F35F50E85F843BA01851E5386B7EBE27367D3D483C5472D3020AF42116DDDA32341557EBABB043EBC6006B99A652009045BFA50C527028586E05942E1D594223B49FE8566931C31FBE8C903ABD4F283E1FAB03D758247EC4B728A85A9897601B753293263ADBD10BE988D0C52FE0091C2721DC02C5130FC7663E95739A70EE2F84DFD2E50C88A1A26587EF7CC047FCA2D03C2CF0CE4B524B4EC3F0703";
//        String result = bytes2hex(envelope.sha1(hexStringToBytes(s)));
//        System.out.println("-------->" + result);

//        String encryptData = "92583a07f6280625ee4ca043e3245f2cd6cca8bae6e198f4046a5dde055723d2591a84ddca4d7f7bb1b179881fd9ec4e33ed22333a9008daeb3c3b1d7143d1953f2363bea4c0d2592667c3468f228f856a95a6dca1fa9ca0ab05d25dc612e7e2bf2ae3012d22c78bb7224c8c8e02146929937c3df9fa3589b2a486c132477acfa50be09528fcbfda43079af54c050843be4bde701d246d8d8a4c947f12afd97a66010459bbae4ed627f687cc3e6dc30b5b35fe3564d9fb07f501b57a73a70ab9c3398e14391b16a5fe45c374984219f0b3a3265a82d3f5a48ceef3998dcea59f1cc5821b51605c66c8fd2687778c84b51cce51c1fbfa876f978e0a9546c425ff";
//        String signData = "3BA0F51DC5B3400E5CD29429663008713C3B61DE0C053590296421635218AEB228A1802C971B18CCF0A137D66FE07B08A0B2A592F11557CC401C353C859E1B82C4BAE146F8AC2955BD1326A3482B173E5589B321FBA0517DCA071F120D0940DC7B8CD33C861E1403CCBD7C3203F1609D261D38B415A0BF234CC9370D18B1004D89BE4C7C4631C7A5D3A1010F0371E25F70B8000D5B94C946571D0F6A730DEF57950AED18839B38B0FF6497D03E960194CF3F113C57575F62E8299FCDE855A1BD36ECE5CAF3DC9F942387A76A329715EC09FDBED3C4FACA06160D538EC00D0166D46152D61F6C665F749E91A0E70E532CE726525B946ACD81510FF47146F00994";
//        String encryptDataWith3des = "5816df38aec7c0e569c011db7212278a767c8934770c7e994e9508e256b693973fbb4b47a78a9f6b1ab2d326cc2a76a53e3731b8a8128b1de4bedcca51e0e740c1a474c21c8cf4a4726f4fbe0dc5ce41c4db7a2cdbb2ef7b2c0f61b50e34a1a327a5069eb23524db0d8119c4c407b90277b806288ecac2826af8af6d092b29e90c03554986f38345b6bb247bc1498c2185661bde318adecaf199e798d70a058305f686ecc3a267d28eed6052483401eb5b5b84f897caea7968b8eeab23f465ce3f1e7f7f7e402d1aa681d76d34cf9ec0b6bbbe9a513b8c42e5ea5319e218ac996f87767966dbd8f8";
//        String digit = Envelope.getDigitalEnvelopStr(encryptData,encryptDataWith3des,"01","A0818e301806092a864886f70d010903310b06092a864886f70d0107033020060a2a864886f70d01091903311204104cdcedd916aaaceeae548a1c5b0a0eaa301f06092a864886f70d0107013112041041303031364b30544e30304530303030302f06092a864886f70d01090431220420a0e06a133da8d4a5ec5a2e51e468b470b19e13834019a0c2563ba39308660a1f",signData,"");

    }

    public static String getDigitalEnvelopStr(InputStream stream,String serialNumber ,String counter, String devicePosPublicKey) {
        //该方法是为了生成token激活代码，做spoc认证监管控制
        try{
            Envelope envelope = new Envelope();
            serialNumber=serialNumber.substring(10);
            String strRand="" ;
            for(int i=0;i<10;i++){
                strRand += String.valueOf((int)(Math.random() * 10));
            }
            String randomeNumber = String.valueOf(strRand);
            System.out.println(randomeNumber);
            System.out.println("serialNumber : " + serialNumber);

            String key_str="0F".concat(serialNumber).concat(counter).concat(randomeNumber).concat("02012C");
//            String key_str="0F230518002300000000028358201e65020060";
            System.out.println("key_str : " + key_str);

            byte[] setTokenStrData = new byte[1 + 4 + key_str.length() / 2];
            setTokenStrData[0] = 0x00;
            byte[] lenBytes = Utils.int2Byte(key_str.length() / 2);
            System.arraycopy(lenBytes, 0, setTokenStrData, 1, lenBytes.length);

            byte[] tokenBytes = hexStringToBytes(key_str);
            System.arraycopy(tokenBytes, 0, setTokenStrData, 1 + lenBytes.length, tokenBytes.length);

            System.out.println("setTokenStrData: "+bytes2hex(setTokenStrData));
            byte[] command = new byte[]{0x01,0x03,0x00,0x00};
            byte[] message2 = new byte[4 + 4 + setTokenStrData.length];
            System.arraycopy(command, 0, message2, 0, command.length);
            lenBytes = Utils.int2Byte(setTokenStrData.length);
            System.arraycopy(lenBytes, 0, message2, 0 + command.length, lenBytes.length);
            System.arraycopy(setTokenStrData, 0, message2, 0 + command.length + lenBytes.length, setTokenStrData.length);
            System.out.println("message2: "+bytes2hex(message2));
            RSA senderRsa = new RSA();
            RSA receiverRsa = new RSA();
            senderRsa.loadPrivateKey(stream);
            //String devicePosPublicKey =
            //"A585B766F09C2BAD3926DB861D633E41581E2A7E75B637F69F0C888EACDAD1761E1B8E7E36A4FFABF6DC8B69EE2429D8D8AA69E070829DC2601E8567833D6E8DEF3898BF0F0CF01C6350D2746DAE282B967E1520A2CAC28261880031BF0C73CB81B753377C174D43DB9354DBD02B15D74F000C4B9C3EA814E4F326CCB1A90849";

            //senderRsa.loadPrivateKey(privateKey);

            String e = "010001";
            receiverRsa.loadPublicKey(devicePosPublicKey, e);

            byte[] de = byteTokenEvelope(message2, senderRsa, receiverRsa,2048);
            System.out.println("de: "+bytes2hex(de));
            int blockSize = de.length / 256;
            if((de.length % 256) != 0){
                blockSize += 1;
            }

            byte[] pde = new byte[blockSize * 256];
            for (int i = 0; i < pde.length; i++) {
                pde[i] = (byte)0xFF;
            }
            for(int i=0;i<10000;i++)
            {
                i+=1;
            }
            System.arraycopy(de, 0, pde, 0, de.length);

            digitalEnvelopStr = bytes2hex(pde);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return  digitalEnvelopStr;
    }

    /*
     * in privateKey inputstream
     *
     * */


    public static String getDigitalEnvelopStrByKey(InputStream in, Poskeys posKeys, Poskeys.RSA_KEY_LEN rsa_key_len , int keyIndex) {
        String ipekKeyStr = null;
        try {
            if (posKeys instanceof DukptKeys) {
                DukptKeys dukptKeys = (DukptKeys) posKeys;
                String trackipekString = dukptKeys.getTrackipek();
                String emvipekString = dukptKeys.getEmvipek();
                String pinipekString = dukptKeys.getPinipek();
                byte[] trackipek = hexStringToBytes(trackipekString);
                byte[] emvipek = hexStringToBytes(emvipekString);
                byte[] pinipek = hexStringToBytes(pinipekString);
                String trackksn = dukptKeys.getTrackksn();
                String emvksn = dukptKeys.getEmvksn();
                String pinksn = dukptKeys.getPinksn();
                String tmkString = dukptKeys.getTmk();
                byte[] tmk = hexStringToBytes(tmkString);
                String trackipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, trackipek));
                String emvipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, emvipek));
                String pinipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, pinipek));
                ipekKeyStr = trackksn + trackipek1 + emvksn + emvipek1 + pinksn + pinipek1;
            } else if (posKeys instanceof TMKKey) {
                TMKKey tmkKey = (TMKKey) posKeys;
                ipekKeyStr = tmkKey.getTMKKEY();
            } else if(posKeys instanceof UserRsaPublickeys){
                UserRsaPublickeys userRsaPublickeys = (UserRsaPublickeys) posKeys;
                ipekKeyStr = userRsaPublickeys.getPublicKey();
            }
            //else if (posKeys instanceof DukptKeys_spe) {
            //     DukptKeys_spe dukptKeys = (DukptKeys_spe) posKeys;
            //     String trackipekString = dukptKeys.getTrackipek();
            //     String emvipekString = dukptKeys.getEmvipek();
            //     String pinipekString = dukptKeys.getPinipek();
            //     String pinkey_covertString = dukptKeys.getPinkey_covert();
            //     byte[] trackipek = hexStringToBytes(trackipekString);
            //     byte[] emvipek = hexStringToBytes(emvipekString);
            //     byte[] pinipek = hexStringToBytes(pinipekString);
            //     byte[] pinkey_covert = hexStringToBytes(pinkey_covertString);
            //     String trackksn = dukptKeys.getTrackksn();
            //     String emvksn = dukptKeys.getEmvksn();
            //     String pinksn = dukptKeys.getPinksn();
            //     String pinksn_covert = dukptKeys.getPinksn_covert();
            //     String tmkString = dukptKeys.getTmk();
            //     byte[] tmk = hexStringToBytes(tmkString);
            //     String trackipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, trackipek));
            //     String emvipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, emvipek));
            //     String pinipek1 = bytes2hex(TDES.tdesECBDecrypt(tmk, pinipek));
            //     String pinkey_covert1 = bytes2hex(TDES.tdesECBDecrypt(tmk, pinkey_covert));
            //     ipekKeyStr = trackksn + trackipek1 + emvksn + emvipek1 + pinksn + pinipek1+pinksn_covert+pinkey_covert1;
            // }

            byte[] setIpekKeyStrData = new byte[5 + ipekKeyStr.length() / 2+1];
            if (posKeys instanceof DukptKeys)
                setIpekKeyStrData[0] = 5;
            else if (posKeys instanceof TMKKey)
                setIpekKeyStrData[0] = 4;
            else if(posKeys instanceof UserRsaPublickeys){
                setIpekKeyStrData[0] = 0;
            }
            //else if (posKeys instanceof DukptKeys_spe)
            //    setIpekKeyStrData[0] = 6;

            byte[] lenBytes = Utils.int2Byte(ipekKeyStr.length() / 2+1);
            System.arraycopy(lenBytes, 0, setIpekKeyStrData, 1, lenBytes.length);
            byte[] ipekBytes = hexStringToBytes(ipekKeyStr);
            byte bytKeyIndex = (byte) keyIndex;
            if (posKeys instanceof DukptKeys ){
                System.arraycopy(ipekBytes, 0, setIpekKeyStrData, 1 + lenBytes.length, ipekBytes.length);
                setIpekKeyStrData[setIpekKeyStrData.length -1] = bytKeyIndex;
            } else if (posKeys instanceof TMKKey){
                setIpekKeyStrData[5] = bytKeyIndex;
                System.arraycopy(ipekBytes, 0, setIpekKeyStrData, 1 + lenBytes.length+1, ipekBytes.length);
            } else if( posKeys instanceof UserRsaPublickeys){
                System.arraycopy(ipekBytes, 0, setIpekKeyStrData, 1 + lenBytes.length, ipekBytes.length);
            }


            byte[] command;
            if(posKeys instanceof UserRsaPublickeys){
                command = new byte[]{1, 1, 0, 0};
            }else {
                command = new byte[]{1, 2, 0, 0};
            }
            byte[] message2 = new byte[8 + setIpekKeyStrData.length];
            System.arraycopy(command, 0, message2, 0, command.length);
            lenBytes = Utils.int2Byte(setIpekKeyStrData.length);
            System.arraycopy(lenBytes, 0, message2,  command.length, lenBytes.length);
            System.arraycopy(setIpekKeyStrData, 0, message2,  command.length + lenBytes.length, setIpekKeyStrData.length);


            RSA senderRsa = new RSA();
            RSA receiverRsa = new RSA();
            senderRsa.loadPrivateKey(in); //私钥
            String n = posKeys.getRSA_public_key();
            String e = "010001";
            receiverRsa.loadPublicKey(n, e);//公钥

            return packageEnvelopFun(message2,senderRsa,receiverRsa,rsa_key_len);
        } catch (Exception e) {
            return digitalEnvelopStr;
        }
    }

    public static String getDigitalEnvelopStr(String encryptData,String encryptDataWith3des, String keyType,String clearData,String signData,String IV){
        int encryptDataLen = (encryptData.length()/2);
        int encryptDataWith3desLen = (encryptDataWith3des.length()/2);
        int clearDataLen = (clearData.length()/2);
        int signDataLen = (signData.length()/2);
        int ivLen = IV.length()/2;
        int len = 2 + 1 + 2+2+ encryptDataLen+2+encryptDataWith3desLen+1+ivLen+1+2+clearDataLen+2+signDataLen;
        String len2= QPOSUtil.byteArray2Hex(Util.intToByte(len));
        String result = len2+"010000"+ QPOSUtil.intToHex2(encryptDataLen)+encryptData+QPOSUtil.intToHex2(encryptDataWith3desLen)+encryptDataWith3des
                +"0"+Integer.toString(ivLen,16)+IV
                +keyType+ QPOSUtil.intToHex2(clearDataLen)+clearData+QPOSUtil.intToHex2(signDataLen)+signData;
        System.out.println("sys = "+result);
        return result;
    }

    private static String packageEnvelopFun(byte[] message2,RSA senderRsa,RSA receiverRsa,Poskeys.RSA_KEY_LEN rsa_key_len) {
        try{

            byte[] de = null;
            if (rsa_key_len == Poskeys.RSA_KEY_LEN.RSA_KEY_1024)
                de = byteEvelope(message2, senderRsa, receiverRsa);
            else if (rsa_key_len == Poskeys.RSA_KEY_LEN.RSA_KEY_2048)
                de = byteEvelope(message2, senderRsa, receiverRsa, 2048);
            else {
                throw new Exception("Bad key length");
            }
            int blockSize = de.length / 256;
            if (de.length % 256 != 0) {
                ++blockSize;
            }

            byte[] pde = new byte[blockSize * 256];

            int i;
            for (i = 0; i < pde.length; ++i) {
                pde[i] = -1;
            }

            for (i = 0; i < 10000; ++i) {
                ++i;
            }

            System.arraycopy(de, 0, pde, 0, de.length);
            System.out.println("de:" + de.length + "\n" + "pde:" + pde.length);
            System.out.println(bytes2hex(pde));
            digitalEnvelopStr = bytes2hex(pde);
            System.out.println("length:" + bytes2hex(pde).length());
            System.out.println("digitalEnvelopStr:" + digitalEnvelopStr);
            return digitalEnvelopStr;
        } catch (Exception var30) {
            var30.printStackTrace();
            return digitalEnvelopStr;
        }
    }

    public static String getDigitalEnvelopStr(InputStream in, DukptKeys dukptKeys, Poskeys.RSA_KEY_LEN rsa_key_len) {
        return getDigitalEnvelopStrByKey(in,dukptKeys,rsa_key_len,0);
    }

    public static String getDigitalEnvelopStr(InputStream in, UserRsaPublickeys dukptKeys, Poskeys.RSA_KEY_LEN rsa_key_len) {
        return getDigitalEnvelopStrByKey(in,dukptKeys,rsa_key_len,0);
    }

    public static String getDigitalEnvelopStr(InputStream in, DukptKeys dukptKeys) {
        return getDigitalEnvelopStr(in, dukptKeys, DukptKeys.RSA_KEY_LEN.RSA_KEY_1024);
    }

}