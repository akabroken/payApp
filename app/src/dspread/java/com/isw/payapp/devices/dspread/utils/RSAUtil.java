package com.isw.payapp.devices.dspread.utils;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
public class RSAUtil {
    public static final String AES_CBC_NoPadding = "AES/CBC/NoPadding";
    public static final String AES_CBC_PKCS5Padding = "AES/CBC/PKCS5Padding";
    public static final String AES_ECB_NoPadding = "AES/ECB/NoPadding";
    public static final String AES_ECB_PKCS5Padding = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "DESede";
    public static final String DES_CBC_NoPadding = " DES/CBC/NoPadding";
    public static final String DES_CBC_PKCS5Padding = "DES/CBC/PKCS5Padding";
    public static final String DES_ECB_NoPadding = "DES/ECB/NoPadding";
    public static final String DES_ECB_PKCS5Padding = "DES/ECB/PKCS5Padding";
    public static final String DESede_CBC_NoPadding = "DESede/CBC/NoPadding";
    public static final String DESede_CBC_PKCS5Padding = "DESede/CBC/PKCS5Padding";
    public static final String DESede_ECB_NoPadding = "DESede/ECB/NoPadding";
    public static final String DESede_ECB_PKCS5Padding = "DESede/ECB/PKCS5Padding";
    public static final String RSA_ECB_OAEPWithSHA1AndMGF1Padding = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    public static final String RSA_ECB_OAEPWithSHA256AndMGF1Padding = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA_ECB_PKCS1Padding = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION = "DESede/ECB/NoPadding";

    public static byte[] EncriptacionTripleDES(final String s, final byte[] array, final byte[] array2) throws Exception {
        final Cipher instance = Cipher.getInstance("DESede/ECB/NoPadding");
        instance.init(2, new SecretKeySpec(array, "DESede"));
        return instance.doFinal(array2);
    }

    public static byte[] EncriptacionTripleDES(final byte[] array, final byte[] array2) throws Exception {
        final Cipher instance = Cipher.getInstance("DESede/ECB/NoPadding");
        instance.init(2, new SecretKeySpec(array, "DESede"));
        return instance.doFinal(array2);
    }

    public static byte[] cryptRSA(final PublicKey publicKey, final byte[] array) throws GeneralSecurityException {
        final Cipher instance = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        instance.init(1, publicKey);
        return instance.doFinal(array);
    }

    public static byte[] decryptRSA(final String s, final PrivateKey privateKey, final byte[] array) throws GeneralSecurityException {
        final Cipher instance = Cipher.getInstance(s);
        instance.init(2, privateKey);
        return instance.doFinal(array);
    }

    public static byte[] decryptRSA(final PrivateKey privateKey, final byte[] array) throws GeneralSecurityException {
        final Cipher instance = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        instance.init(Cipher.DECRYPT_MODE, privateKey);
        return instance.doFinal(array);
    }

    public static void evaluateAllPadings(final PrivateKey p0, final byte[] p1) {
        //
        // This method could not be decompiled.
        //
        // Original Bytecode:
        //
        //     2: ldc             "AES/CBC/NoPadding"
        //     4: aload_0
        //     5: aload_1
        //     6: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //     9: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //    12: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //    15: pop
        //    16: ldc             "AES_CBC_PKCS5Padding "
        //    18: ldc             "AES/CBC/PKCS5Padding"
        //    20: aload_0
        //    21: aload_1
        //    22: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    25: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //    28: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //    31: pop
        //    32: ldc             "AES/CBC/PKCS5Padding"
        //    34: aload_0
        //    35: aload_1
        //    36: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    39: pop
        //    40: ldc             "AES_ECB_PKCS5Padding"
        //    42: ldc             "AES/ECB/PKCS5Padding"
        //    44: aload_0
        //    45: aload_1
        //    46: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    49: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //    52: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //    55: pop
        //    56: ldc             "AES/ECB/PKCS5Padding"
        //    58: aload_0
        //    59: aload_1
        //    60: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    63: pop
        //    64: ldc             "DES_CBC_NoPadding"
        //    66: ldc             " DES/CBC/NoPadding"
        //    68: aload_0
        //    69: aload_1
        //    70: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    73: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //    76: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //    79: pop
        //    80: ldc             " DES/CBC/NoPadding"
        //    82: aload_0
        //    83: aload_1
        //    84: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    87: pop
        //    88: ldc             "DES_CBC_PKCS5Padding"
        //    90: ldc             "DES/CBC/PKCS5Padding"
        //    92: aload_0
        //    93: aload_1
        //    94: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //    97: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   100: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   103: pop
        //   104: ldc             "DES/CBC/PKCS5Padding"
        //   106: aload_0
        //   107: aload_1
        //   108: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   111: pop
        //   112: ldc             "DES_ECB_NoPadding"
        //   114: ldc             "DES/ECB/NoPadding"
        //   116: aload_0
        //   117: aload_1
        //   118: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   121: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   124: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   127: pop
        //   128: ldc             "DES/ECB/NoPadding"
        //   130: aload_0
        //   131: aload_1
        //   132: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   135: pop
        //   136: ldc             "DES_ECB_PKCS5Padding"
        //   138: ldc             "DES/ECB/PKCS5Padding"
        //   140: aload_0
        //   141: aload_1
        //   142: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   145: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   148: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   151: pop
        //   152: ldc             "DES/ECB/PKCS5Padding"
        //   154: aload_0
        //   155: aload_1
        //   156: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   159: pop
        //   160: ldc             "DESede_CBC_NoPadding"
        //   162: ldc             "DESede/CBC/NoPadding"
        //   164: aload_0
        //   165: aload_1
        //   166: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   169: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   172: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   175: pop
        //   176: ldc             "DESede/CBC/NoPadding"
        //   178: aload_0
        //   179: aload_1
        //   180: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   183: pop
        //   184: ldc             "DESede_CBC_PKCS5Padding"
        //   186: ldc             "DESede/CBC/PKCS5Padding"
        //   188: aload_0
        //   189: aload_1
        //   190: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   193: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   196: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   199: pop
        //   200: ldc             "DESede/CBC/PKCS5Padding"
        //   202: aload_0
        //   203: aload_1
        //   204: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   207: pop
        //   208: ldc             "DESede_ECB_NoPadding"
        //   210: ldc             "DESede/ECB/NoPadding"
        //   212: aload_0
        //   213: aload_1
        //   214: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   217: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   220: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   223: pop
        //   224: ldc             "DESede/ECB/NoPadding"
        //   226: aload_0
        //   227: aload_1
        //   228: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   231: pop
        //   232: ldc             "DESede_ECB_PKCS5Padding"
        //   234: ldc             "DESede/ECB/PKCS5Padding"
        //   236: aload_0
        //   237: aload_1
        //   238: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   241: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   244: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   247: pop
        //   248: ldc             "DESede/ECB/PKCS5Padding"
        //   250: aload_0
        //   251: aload_1
        //   252: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   255: pop
        //   256: ldc             "RSA_ECB_PKCS1Padding"
        //   258: ldc             "RSA/ECB/PKCS1Padding"
        //   260: aload_0
        //   261: aload_1
        //   262: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   265: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   268: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   271: pop
        //   272: ldc             "RSA/ECB/PKCS1Padding"
        //   274: aload_0
        //   275: aload_1
        //   276: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   279: pop
        //   280: ldc             ""
        //   282: new             Ljava/lang/StringBuilder;
        //   285: dup
        //   286: invokespecial   java/lang/StringBuilder.<init>:()V
        //   289: ldc             "RSA_ECB_OAEPWithSHA1AndMGF1Padding"
        //   291: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   294: ldc             "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
        //   296: aload_0
        //   297: aload_1
        //   298: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   301: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   304: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   307: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   310: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   313: pop
        //   314: ldc             "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
        //   316: aload_0
        //   317: aload_1
        //   318: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   321: pop
        //   322: ldc             ""
        //   324: new             Ljava/lang/StringBuilder;
        //   327: dup
        //   328: invokespecial   java/lang/StringBuilder.<init>:()V
        //   331: ldc             "RSA_ECB_OAEPWithSHA256AndMGF1Padding"
        //   333: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   336: ldc             "AES/CBC/NoPadding"
        //   338: aload_0
        //   339: aload_1
        //   340: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   343: invokestatic    com/dspread/encdata/HexUtil.hexStringFromBytes:([B)Ljava/lang/String;
        //   346: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   349: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   352: invokestatic    android/util/Log.i:(Ljava/lang/String;Ljava/lang/String;)I
        //   355: pop
        //   356: ldc             "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        //   358: aload_0
        //   359: aload_1
        //   360: invokestatic    com/dspread/encdata/RSAUtil.decryptRSA:(Ljava/lang/String;Ljava/security/PrivateKey;[B)[B
        //   363: pop
        //   364: return
        //   365: astore_2
        //   366: ldc             ""
        //   368: new             Ljava/lang/StringBuilder;
        //   371: dup
        //   372: invokespecial   java/lang/StringBuilder.<init>:()V
        //   375: ldc             "AES_CBC_NoPadding"
        //   377: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   380: aload_2
        //   381: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   384: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   387: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   390: pop
        //   391: goto            16
        //   394: astore_2
        //   395: ldc             ""
        //   397: new             Ljava/lang/StringBuilder;
        //   400: dup
        //   401: invokespecial   java/lang/StringBuilder.<init>:()V
        //   404: ldc             "AES_CBC_PKCS5Padding"
        //   406: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   409: aload_2
        //   410: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   413: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   416: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   419: pop
        //   420: goto            40
        //   423: astore_2
        //   424: ldc             ""
        //   426: new             Ljava/lang/StringBuilder;
        //   429: dup
        //   430: invokespecial   java/lang/StringBuilder.<init>:()V
        //   433: ldc             "AES_ECB_PKCS5Padding"
        //   435: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   438: aload_2
        //   439: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   442: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   445: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   448: pop
        //   449: goto            64
        //   452: astore_2
        //   453: ldc             ""
        //   455: new             Ljava/lang/StringBuilder;
        //   458: dup
        //   459: invokespecial   java/lang/StringBuilder.<init>:()V
        //   462: ldc             "DES_CBC_NoPadding"
        //   464: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   467: aload_2
        //   468: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   471: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   474: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   477: pop
        //   478: goto            88
        //   481: astore_2
        //   482: ldc             ""
        //   484: new             Ljava/lang/StringBuilder;
        //   487: dup
        //   488: invokespecial   java/lang/StringBuilder.<init>:()V
        //   491: ldc             "DES_CBC_PKCS5Padding"
        //   493: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   496: aload_2
        //   497: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   500: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   503: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   506: pop
        //   507: goto            112
        //   510: astore_2
        //   511: ldc             ""
        //   513: new             Ljava/lang/StringBuilder;
        //   516: dup
        //   517: invokespecial   java/lang/StringBuilder.<init>:()V
        //   520: ldc             "DES_ECB_NoPadding"
        //   522: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   525: aload_2
        //   526: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   529: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   532: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   535: pop
        //   536: goto            136
        //   539: astore_2
        //   540: ldc             ""
        //   542: new             Ljava/lang/StringBuilder;
        //   545: dup
        //   546: invokespecial   java/lang/StringBuilder.<init>:()V
        //   549: ldc             "DES_ECB_PKCS5Padding"
        //   551: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   554: aload_2
        //   555: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   558: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   561: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   564: pop
        //   565: goto            160
        //   568: astore_2
        //   569: ldc             ""
        //   571: new             Ljava/lang/StringBuilder;
        //   574: dup
        //   575: invokespecial   java/lang/StringBuilder.<init>:()V
        //   578: ldc             "DESede_CBC_NoPadding"
        //   580: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   583: aload_2
        //   584: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   587: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   590: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   593: pop
        //   594: goto            184
        //   597: astore_2
        //   598: ldc             ""
        //   600: new             Ljava/lang/StringBuilder;
        //   603: dup
        //   604: invokespecial   java/lang/StringBuilder.<init>:()V
        //   607: ldc             "DESede_CBC_PKCS5Padding"
        //   609: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   612: aload_2
        //   613: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   616: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   619: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   622: pop
        //   623: goto            208
        //   626: astore_2
        //   627: ldc             ""
        //   629: new             Ljava/lang/StringBuilder;
        //   632: dup
        //   633: invokespecial   java/lang/StringBuilder.<init>:()V
        //   636: ldc             "DESede_ECB_NoPadding"
        //   638: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   641: aload_2
        //   642: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   645: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   648: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   651: pop
        //   652: goto            232
        //   655: astore_2
        //   656: ldc             ""
        //   658: new             Ljava/lang/StringBuilder;
        //   661: dup
        //   662: invokespecial   java/lang/StringBuilder.<init>:()V
        //   665: ldc             "DESede_ECB_PKCS5Padding"
        //   667: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   670: aload_2
        //   671: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   674: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   677: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   680: pop
        //   681: goto            256
        //   684: astore_2
        //   685: ldc             ""
        //   687: new             Ljava/lang/StringBuilder;
        //   690: dup
        //   691: invokespecial   java/lang/StringBuilder.<init>:()V
        //   694: ldc             "RSA_ECB_PKCS1Padding"
        //   696: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   699: aload_2
        //   700: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   703: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   706: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   709: pop
        //   710: goto            280
        //   713: astore_2
        //   714: ldc             ""
        //   716: new             Ljava/lang/StringBuilder;
        //   719: dup
        //   720: invokespecial   java/lang/StringBuilder.<init>:()V
        //   723: ldc             "RSA_ECB_OAEPWithSHA1AndMGF1Padding"
        //   725: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   728: aload_2
        //   729: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   732: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   735: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   738: pop
        //   739: goto            322
        //   742: astore_0
        //   743: ldc             ""
        //   745: new             Ljava/lang/StringBuilder;
        //   748: dup
        //   749: invokespecial   java/lang/StringBuilder.<init>:()V
        //   752: ldc             "RSA_ECB_OAEPWithSHA256AndMGF1Padding"
        //   754: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   757: aload_0
        //   758: invokevirtual   java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
        //   761: invokevirtual   java/lang/StringBuilder.toString:()Ljava/lang/String;
        //   764: invokestatic    android/util/Log.e:(Ljava/lang/String;Ljava/lang/String;)I
        //   767: pop
        //   768: return
        //    Exceptions:
        //  Try           Handler
        //  Start  End    Start  End    Type
        //  -----  -----  -----  -----  ---------------------
        //  0      16     365    394    Ljava/lang/Exception;
        //  16     40     394    423    Ljava/lang/Exception;
        //  40     64     423    452    Ljava/lang/Exception;
        //  64     88     452    481    Ljava/lang/Exception;
        //  88     112    481    510    Ljava/lang/Exception;
        //  112    136    510    539    Ljava/lang/Exception;
        //  136    160    539    568    Ljava/lang/Exception;
        //  160    184    568    597    Ljava/lang/Exception;
        //  184    208    597    626    Ljava/lang/Exception;
        //  208    232    626    655    Ljava/lang/Exception;
        //  232    256    655    684    Ljava/lang/Exception;
        //  256    280    684    713    Ljava/lang/Exception;
        //  280    322    713    742    Ljava/lang/Exception;
        //  322    364    742    769    Ljava/lang/Exception;
        //
        // The error that occurred was:
        //
        // java.lang.IndexOutOfBoundsException: Index: 374, Size: 374
        //     at java.util.ArrayList.rangeCheck(Unknown Source)
        //     at java.util.ArrayList.get(Unknown Source)
        //     at com.strobel.decompiler.ast.AstBuilder.convertToAst(AstBuilder.java:3321)
        //     at com.strobel.decompiler.ast.AstBuilder.build(AstBuilder.java:113)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:210)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:99)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:757)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:655)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:532)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:499)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:141)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:130)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:105)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at us.deathmarine.luyten.DecompilerLinkProvider.generateContent(DecompilerLinkProvider.java:97)
        //     at us.deathmarine.luyten.OpenFile.decompileWithNavigationLinks(OpenFile.java:469)
        //     at us.deathmarine.luyten.OpenFile.decompile(OpenFile.java:442)
        //     at us.deathmarine.luyten.Model.extractClassToTextPane(Model.java:420)
        //     at us.deathmarine.luyten.Model.openEntryByTreePath(Model.java:339)
        //     at us.deathmarine.luyten.Model$TreeListener$1.run(Model.java:266)
        //
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
}
