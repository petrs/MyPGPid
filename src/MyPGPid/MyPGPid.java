/*
 * MyPGPid.java - Java card implementation of openpgp card (main applet).
 *
 * Copyright (C) 2007  Sten Lindgren (initial JOpenPGPCard project)
 *           (C) 2013 Diego 'NdK' Zuccato, (C) 2013 Petr Svenda (MyPGPid project)
 *
 *  This file is part of MyPGPid.
 *
 *  MyPGPid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Package AID:  0xD2:0x76:0x00:0x01:0x24:0x01:0x02:0x00
 * AppletAID:    0xD2:0x76:0x00:0x01:0x24:0x01:0x02:0x00:0x00:0x00:0x00:0x00:0x00:0x01:0x00:0x00
 */


package MyPGPid;


//TODO: implement standard 2.0 put data - private keys (used by GPG)
//TODO: implement export of private key after key generation (to backup card)

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

/**
 *
 * @author Sten, Diego, Petr
 */
public class MyPGPid extends Applet {
    // Odd high nibble in CLA means 'chaining'
    final static byte CLA_CARD_TEST               = (byte) 0xE0;

    final static byte CLA_CARD_NOCHAIN_NOSM       = (byte) 0x00;
    final static byte CLA_CARD_SM                 = (byte) 0x0C;
    final static byte CLA_CARD_CHAIN_NOSM         = (byte) 0x10;
    final static byte CLA_CARD_CHAIN_SM           = (byte) 0x1C;
    final static byte CLA_CHAINING                = (byte) 0x10;
    
    
    /* Commands from terminal */
    final static byte PGP_CLA = (byte)0x00;
    final static byte SELECT_FILE = (byte)0x20;
    final static byte GET_DATA = (byte)0xca;
    final static byte VERIFY = (byte)0x20;
    final static byte CHANGE_REFERENCE_DATA = (byte)0x24;
    final static byte RESET_RETRY_COUNTER = (byte)0x2c;
    final static byte PUT_DATA = (byte)0xda;
    final static byte PUT_DATA_CHAINING = (byte)0xdb;
    final static byte GENERATE_ASYMMETRIC_KEY_PAIR = (byte)0x47;
    final static byte PERFORM_SECURITY_OPERATION = (byte)0x2a;
    final static byte INTERNAL_AUTHENTICATE = (byte)0x88;
    final static byte GET_RESPONSE = (byte)0xc0;
    final static byte GET_CHALLENGE = (byte)0x84;
    final static byte ENVELOPE = (byte)0xc2;

    // final static byte EXPORT_KEY_PAIR = (byte)0x05;

    final static byte INS_CARD_READ_POLICY           = (byte) 0x70;
    final static byte INS_CARD_KEY_PUSH              = (byte) 0x72;
    
    /* Data objects constants */
    final static short DO_OPTIONAL1 = (short)0x0101;
    final static short DO_OPTIONAL2 = (short)0x0102;
    final static short DO_OPTIONAL3 = (short)0x0103;
    final static short DO_OPTIONAL4 = (short)0x0104;
    final static short DO_LOGIN_DATA = (short)0x005e;
    final static short DO_URL = (short)0x5f50;
    final static short DO_CARDHOLDER_DATA = (short)0x0065;
    final static short DO_NAME = (short)0x005b;
    final static short DO_LANGUAGE = (short)0x5f2d;
    final static short DO_SEX = (short)0x5f35;
    final static short DO_HISTORICAL = (short)0x5f52;
    final static short DO_APPLICATION_DATA = (short)0x006E;
    final static short DO_AID = (short)0x004f;
    final static short DO_DISCRETIONARY_DATA = (short)0x0073;
    final static short DO_EXTENDED_CAPABILITIES = (short)0x00c0;
    final static short DO_ALG_ATTR_SIGN = (short)0x00c1;
    final static short DO_ALG_ATTR_DEC = (short)0x00c2;
    final static short DO_ALG_ATTR_AUTH = (short)0x00c3;
    final static short DO_CHV_STATUS = (short)0x00c4;
    final static short DO_FINGERPRINTES = (short)0x00c5;
    final static short DO_CA_FINGERPRINTS = (short)0x00c6;
    final static short DO_GENERATION_DATES = (short)0x00cd;
    final static short DO_SECURITY_SUPPORT_TEMPLATE = (short)0x007a;
    final static short DO_SIGNATURE_COUNTER = (short)0x0093;
    final static short DO_READ_ALL = (short)0x00FF;
    final static short DO_FINGERPRINT_SIGN = (short)0x00c7;
    final static short DO_FINGERPRINT_DEC = (short)0x00c8;
    final static short DO_FINGERPRINT_AUTH = (short)0x00c9;
    final static short DO_CA_FINGERPRINT1 = (short)0x00ca;
    final static short DO_CA_FINGERPRINT2 = (short)0x00cb;
    final static short DO_CA_FINGERPRINT3 = (short)0x00cc;
    final static short DO_GENERATION_DATE_SIGN = (short)0x00ce;
    final static short DO_GENERATION_DATE_DEC = (short)0x00cf;
    final static short DO_GENERATION_DATE_AUTH = (short)0x00d0;
    final static short DO_PRIVATE_SIGNATURE_KEY = (short)0x00e0;
    final static short DO_PRIVATE_DECRYPTION_KEY = (short)0x00e1;
    final static short DO_PRIVATE_AUTHENTIFICATION__KEY = (short)0x00e2;
    final static short DO_KEY_IMPORT = (short)0x3fff;

        
    /* Response codes */
    final static short SW_PIN_BLOCKED = (short)0x6983;
    final static short SW_REFERENCED_DATA_NOT_FOUND = (short)0x6a88;
    final static short SW_MORE_DATA = (short)0x6100;
    final static short SW_SUCCESS = (short)0x9000;
    /* Constants and default values */

    final static byte CHV1_LENGTH = (byte)32;
    final static byte CHV2_LENGTH = (byte)32;
    final static byte CHV3_LENGTH = (byte)32;
    final static byte CHV_RETRY = (byte)3;
    final static byte[] DEFAULT_CHV1 = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36 };
    final static byte[] DEFAULT_CHV2 = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36 };
    final static byte[] DEFAULT_CHV3 = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38 };

    final static byte[] DEFAULT_AID = {(byte)0xD2, (byte)0x76, (byte)0x00, (byte)0x01, (byte)0x24, (byte)0x02, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00};


    private OwnerPIN chv1;
    private OwnerPIN chv2;
    private OwnerPIN chv3;
    private DataObject optionalData1;
    private DataObject optionalData2;
    private DataObject optionalData3;
    private DataObject optionalData4;
    private DataObject loginData;
    private DataObject url;
    private DataObject name;
    private DataObject langPref;
    private DataObject sex;
    private DataObject aid;
    // extendedCap[3] == 0x10 (Status byte changeable only) 
    // extendedCap[3] == 0x34 (Support for Key Import, Status byte changeable only, Algorithm attributes changeable) 
    // extendedCap[3] == 0xB4 (Secure Messaging supported, Support for Key Import, Status byte changeable only, Algorithm attributes changeable) 
    private byte[] extendedCap = { (byte)0xc0, (byte)0x0a, (byte)0x34, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0x00, (byte)0xFF};
    // BUGBUG: algAttrSign[7] == 0x01 will cause general fail during key generation (in PGP) 
//    private byte[] algAttrSign = { (byte)0xc1, (byte)0x06, (byte)0x01,(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x01};
//    private byte[] algAttrDec = { (byte)0xc2, (byte)0x06, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x01};
//    private byte[] algAttrAuth = { (byte)0xc3, (byte)0x06, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x01};
    private byte[] algAttrSign = { (byte)0xc1, (byte)0x05, (byte)0x01,(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20};
    private byte[] algAttrDec = { (byte)0xc2, (byte)0x05, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20};
    private byte[] algAttrAuth = { (byte)0xc3, (byte)0x05, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20};
    // Card capabilities are encoded in historical bytes as defined 8.3.6 Card capabilities (http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_8_historical_bytes.aspx)
    //private byte[] histBytes = { (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x00, (byte)0x40, (byte)0x00, (byte)0x90, (byte)0x00}; // 0x80 == COMPACT-TLV, 0x73 == Card capabilities & 3bytes length, 0x40 == extended lc/le supported, 0x00 == status indicator (Card does not offer life cycle management), 90 00 status OK
    private byte[] histBytes = { (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x00, (byte)0x90, (byte)0x00}; // 0x80 == COMPACT-TLV, 0x73 == Card capabilities & 3bytes length, 0x80 == command chaining supported, 0x00 == status indicator (Card does not offer life cycle management), 90 00 status OK
    //private byte[] histBytes = { (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x90, (byte)0x00}; // this will fail (gpg keytocard) as neither extended apdu, nor command chaining is declared for support 0x80 == COMPACT-TLV, 0x73 == Card capabilities & 3bytes length, 0x00 == extended lc/le supported, 0x00 == status indicator (Card does not offer life cycle management), 90 00 status OK
    private DataObject fingerprints;
    private DataObject fingerprintsCA;
    private DataObject dateGeneration;
    private DataObject signCount;
    private KeyPair keySign;
    private KeyPair keyDecrypt;
    private KeyPair keyAuth;
    private Cipher sig;
    private Cipher cipher;
    private RandomData random;
    /* tmpData is used for temporary data when processing. It must be used as
     * output buffer when sending more then 254 bytes of data to the terminal
     * for GET_RESPONSE to work. If used to send data it must not be modified
     * until all data has been sent or a command other then GET_RESPONSE is
     * issued from the terminal.
     */
    private byte[] tmpData;
    private short remainingDataLength = 0;
    private short remainingDataOffset = 0;
    private short incomingDataOffset = 0;
    private boolean chv = false;
    private short bNotExported = 0x55;
 
    
    // *** Extended function support
    private KeyPair[]	m_keyPair = null;	// Keeps all key pairs
    private byte[]	m_transferBuffer = null;
    private byte	m_extFlag = (byte)0;// Using extensions
    private byte	m_currEncKey = 1;
    private byte	m_nPins = 2;
    private short	m_maxKeys = 3;	// SIG,DEC,AUT
    private short	m_bSize = 0;	// buffer size
    private boolean	m_separateAuth = false;// Use different AUT key/PIN for RFID
    private byte	m_oobAuth = 0;	// Require OOB auth for SIG key
    private boolean	m_CAKeyIsSet = false; // True when CA key is set
    private PublicKey	m_CAKey = null;    
    
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new MyPGPid(bArray, bOffset, bLength);
    }

    protected MyPGPid(byte[] buffer, short offset, byte length) {
	// G+D uses non-zero offset, JCOP uses offset=0
        if (length > 9) {
    	    // data offset is used for application specific parameter.
    	    // initialization with default offset (AID offset).
    	    short dataOffset = offset;

            // Install parameter detail. Compliant with OP 2.0.1.
            // | size | content
            // |------|---------------------------
            // |  1   | [AID_Length]
            // | 5-16 | [AID_Bytes]
            // |  1   | [Privilege_Length]
            // | 1-n  | [Privilege_Bytes] (normally 1Byte)
            // |  1   | [Application_Proprietary_Length]
            // | 0-m  | [Application_Proprietary_Bytes]

            // shift to privilege offset
            dataOffset += (short)( 1 + buffer[dataOffset]);
            // finally shift to Application specific offset
            dataOffset += (short)( 1 + buffer[dataOffset]);

	    // Here dataOffset points to policy (app. proprietary) len
/*
	    if((short)(offset+length)>dataOffset) {	// Policy is present
		// Application_Proprietary_Length must be 5
		// Application_Proprietary_Bytes is:
		// | size | content
		// |------|-----------------------
		// |  1   | Number of expired ENC keys to keep
		// |  2   | Max size of transfer buffer (in bytes)
		// |  1   | Use different PIN and key for RFID
		// |  1   | OOB user consent needed
		short polLen=buffer[dataOffset];
                if(((short)(dataOffset+polLen-offset) > length) || (polLen != 5)) { // Check if policy is complete
                    ISOException.throwIt( ISO7816.SW_DATA_INVALID );
                }
                ++dataOffset;

                // Limit of 255 old ENC keys
                m_maxKeys+=buffer[dataOffset];
                ++dataOffset;

                m_bSize=(short)Util.makeShort(buffer[dataOffset], buffer[(short)(dataOffset+1)]);
                ++dataOffset; ++dataOffset;

                if(0!=(byte)buffer[dataOffset]) {
                    m_separateAuth=true;
                    ++m_maxKeys;	// Requires an extra key...
                    ++m_nPins;		// ... and an extra PIN
                }
                ++dataOffset;

                if(0!=buffer[dataOffset]) {
                    // Needs card-specific code!
                    ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED ) ;
                    // m_oobAuth=buffer[dataOffset];
                }

                ++dataOffset;

                // *** These can fail: if so, then abort install

                // To RAM: prevent EEPROM wearout and clear buffer when card is removed
                m_transferBuffer = JCSystem.makeTransientByteArray(m_bSize, JCSystem.CLEAR_ON_DESELECT);

                // Pre-allocate space for all keys
                //@@@ Should allocate space for key IDs too!
                m_keyPair = new KeyPair[m_maxKeys];
                for(short c=0; c<m_maxKeys; ++c)
                    m_keyPair[c]=new KeyPair(DEF_ALG, DEF_LEN);
                m_CAKey= (PublicKey)KeyBuilder.buildKey( DEF_CAKEYTYPE, DEF_LEN, false);
	    }
*/
            
            // ORIGINAL INIT CODE FROM JOpenPGPCard
            loginData = new DataObject((byte)0x00, (byte)0x5e, (short)254);
            url = new DataObject((byte)0x5f, (byte)0x50, (short)254);
            name = new DataObject((byte)0x5b, (short)39);
            langPref = new DataObject((byte)0x5f, (byte)2d, (short)8);
            sex = new DataObject((byte)0x5f, (byte)35, (short)1);
            aid = new DataObject((byte)0x4f, (short)16);
            fingerprints = new DataObject((byte)00, (byte)0xc5, (short)60, true);
            fingerprintsCA = new DataObject((byte)00, (byte)0xc6, (short)60, true);
            dateGeneration = new DataObject((byte)00, (byte)0xcd, (short)12, true);
            signCount = new DataObject((byte)0, (byte)0x93, (short)3, true);
            keySign = new KeyPair(DEF_ALG(), DEF_KEY_LEN());
            keyDecrypt = new KeyPair(DEF_ALG(), DEF_KEY_LEN());
            keyAuth = new KeyPair(DEF_ALG(), DEF_KEY_LEN());
            chv1 = new OwnerPIN(CHV_RETRY, CHV1_LENGTH);
            chv2 = new OwnerPIN(CHV_RETRY, CHV2_LENGTH);
            chv3 = new OwnerPIN(CHV_RETRY, CHV3_LENGTH);

            chv1.update(DEFAULT_CHV1, (byte)0, (byte)6);
            chv2.update(DEFAULT_CHV2, (byte)0, (byte)6);
            chv3.update(DEFAULT_CHV3, (byte)0, (byte)8);
            sig = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
            cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
            random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
            tmpData = JCSystem.makeTransientByteArray((short)512, JCSystem.CLEAR_ON_DESELECT);

            // Set proper Algorithm Attributes according to defined preferences
            Util.setShort(algAttrSign, (short)3, DEF_KEY_LEN());
            Util.setShort(algAttrDec, (short)3, DEF_KEY_LEN());
            Util.setShort(algAttrAuth, (short)3, DEF_KEY_LEN());
            algAttrSign[5] = (DEF_ALG() == KeyPair.ALG_RSA) ? (byte) 0x01 : (byte) 0x03;
            algAttrDec[5] = (DEF_ALG() == KeyPair.ALG_RSA) ? (byte) 0x01 : (byte) 0x03;
            algAttrAuth[5] = (DEF_ALG() == KeyPair.ALG_RSA) ? (byte) 0x01 : (byte) 0x03;    
            // END ORIGINAL CODE FROM JOpenPGPCard        

            
	    // Register OP2 applet
	    register(buffer, (short)(offset + 1), (byte)buffer[offset]);
        } else {
	    // Register non-OP2 applet
	    register();
	}               
    }

    // *** Card-specific configuration - methods are overridden by card specific driver
    public static short BuffSize() {return (short)0x300;};
    public static byte UserConsent() {return (byte)0; };
    public static byte DEF_ALG() { return KeyPair.ALG_RSA_CRT; };
    public static short DEF_KEY_LEN() { return (short) 2048; };
    public static byte DEF_CAKEYTYPE() {return KeyBuilder.TYPE_RSA_PUBLIC; };
    
    public void deselect() {
        chv1.reset();
        chv2.reset();
        chv3.reset();
        remainingDataLength = 0;
        remainingDataOffset = 0;
        incomingDataOffset = 0;
    }

    public boolean select() {
        deselect(); // Lets be paranoid and reset pins etc at select too.
        return true;
    }

    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc;
        boolean status = false;

        // ignore the applet select command dispached to the process
        if (selectingApplet()) {
            return;
        }

        buffer[ISO7816.OFFSET_CLA] = (byte)(buffer[ISO7816.OFFSET_CLA] & (byte)0xFC);

        if (buffer[ISO7816.OFFSET_INS] == GET_RESPONSE) {
            if (remainingDataLength <= 0) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            else { sendData(apdu, tmpData, remainingDataLength);} 
            return;
        } else {
            remainingDataLength = 0;
            remainingDataOffset = 0;
        }
/**/
        switch (buffer[ISO7816.OFFSET_INS]) {
            case ISO7816.INS_SELECT:
                return;
            case GET_DATA:
               getData(apdu);
               return;
            case PUT_DATA:
                putData(apdu);
                return;
            case PUT_DATA_CHAINING:
                putDataChaining(apdu);
                return;
            case VERIFY:
                if (buffer[ISO7816.OFFSET_P1] != 0) { ISOException.throwIt(ISO7816.SW_WRONG_P1P2); }
                lc = apdu.setIncomingAndReceive();
                if (lc == 0) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                switch (buffer[ISO7816.OFFSET_P2]) {
                    case (byte)0x81:
                        if (chv1.getTriesRemaining() == (byte)0) {
                            ISOException.throwIt(SW_PIN_BLOCKED);
                        }
                        status = chv1.check(buffer, (short)ISO7816.OFFSET_CDATA, (byte)lc);
                        break;
                    case (byte)0x82:
                        if (chv2.getTriesRemaining() == (byte)0) {
                            ISOException.throwIt(SW_PIN_BLOCKED);
                        }
                        status = chv2.check(buffer, (short)ISO7816.OFFSET_CDATA, (byte)lc);
                        break;
                    case (byte)0x83:
                        if (chv3.getTriesRemaining() == (byte)0) {
                            ISOException.throwIt(SW_PIN_BLOCKED);
                        }
                        status = chv3.check(buffer, (short)ISO7816.OFFSET_CDATA, (byte)lc);
                        break;
                    default:
                        ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                if (!status) { ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);}
                return;
            case GENERATE_ASYMMETRIC_KEY_PAIR:
                generateAssymetricKeyPair(apdu);
                return;
            case PERFORM_SECURITY_OPERATION:
                performSecurityOperation(apdu);
                return;
            case CHANGE_REFERENCE_DATA:
                /* Fall through */
            case RESET_RETRY_COUNTER:
                changeResetChv(apdu);
                return;
            case INTERNAL_AUTHENTICATE:
                if (buffer[ISO7816.OFFSET_P1] != 0 || buffer[ISO7816.OFFSET_P2]
                        != 0) {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                if (!chv2.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                lc = receiveData(apdu, tmpData);
                sig.init(keyAuth.getPrivate(), Cipher.MODE_ENCRYPT);
                lc = sig.doFinal(tmpData, (short)0, lc, tmpData, (short)0);
                sendData(apdu, tmpData, lc);
                return;
            case GET_CHALLENGE:
                if (buffer[ISO7816.OFFSET_P1] != 0 || buffer[ISO7816.OFFSET_P2]
                        != 0) {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                lc = apdu.setOutgoing();
                random.generateData(tmpData, (short) 0, lc);
                apdu.setOutgoingLength(lc);
                apdu.sendBytesLong(tmpData, (short) 0, lc);
                return;
            //case EXPORT_KEY_PAIR:
                //exportKeyPair(apdu);
                //return;

            case INS_CARD_READ_POLICY:
                ReadPolicy(apdu); 
                return;
            case INS_CARD_KEY_PUSH:
                KeyPush(apdu); 
                return;            
            
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();

        short p1p2 = Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]);
        short len;

        switch (p1p2) {
            case DO_LOGIN_DATA:
              len = loginData.getData(buffer, (byte)0);
              apdu.setOutgoingAndSend((short)0, (short)(len));

                return;
            case DO_URL:
                sendData(apdu, url.getData(), (short)url.getDataLength());
                return;
            case DO_NAME:
                len = name.getData(buffer, (byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_LANGUAGE:
                len = langPref.getData(buffer, (byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_SEX:
                buffer[0] = sex.getData()[0];
                apdu.setOutgoingAndSend((short)0, (short)1);
                return;
            case DO_HISTORICAL:
                Util.arrayCopy(histBytes, (short)0, buffer, (short)0, (short) (histBytes.length));
                apdu.setOutgoingAndSend((short)0, (short) (histBytes.length));
                return;
            case DO_CARDHOLDER_DATA:
                len = getCardholderData(tmpData, (byte)0);
                sendData(apdu, tmpData, len);
                return;
            case DO_APPLICATION_DATA:
                tmpData[0] = DO_AID;
                len = JCSystem.getAID().getBytes(tmpData, (short)1);
                tmpData[1] = (byte)len;
                len += 2;
                len = getDiscData(tmpData, len);
                len = loginData.getTlv(tmpData, len);
                sendData(apdu, tmpData, len);
                //apdu.setOutgoingAndSend((short)0, (short)len);

                return;
            case DO_AID:
                len = JCSystem.getAID().getBytes(buffer, (short)0);
                apdu.setOutgoingAndSend((short)0, (short) DEFAULT_AID.length);
//                Util.arrayCopyNonAtomic( DEFAULT_AID, (short) 0, tmpData, (short) 0, (short) DEFAULT_AID.length);
                //sendData(apdu, tmpData, (short) DEFAULT_AID.length);
                return;
            case DO_EXTENDED_CAPABILITIES:
                Util.arrayCopy(extendedCap, (short)2, buffer, (short)0, (short) (extendedCap.length - 2));
                apdu.setOutgoingAndSend((short)0, (short)1);
                return;
            case DO_ALG_ATTR_SIGN:
                Util.arrayCopy(algAttrSign, (short)2, buffer, (short)0, (short) (algAttrSign.length - 2));
                apdu.setOutgoingAndSend((short)0, (short)5);
                return;
            case DO_ALG_ATTR_DEC:
                Util.arrayCopy(algAttrDec, (short)2, buffer, (short)0,(short) (algAttrDec.length - 2));
                apdu.setOutgoingAndSend((short)0, (short)5);
                return;
            case DO_ALG_ATTR_AUTH:
                Util.arrayCopy(algAttrAuth, (short)2, buffer, (short)0, (short) (algAttrAuth.length - 2));
                apdu.setOutgoingAndSend((short)0, (short)5);
                return;
            case DO_CHV_STATUS:
                len = getCHVStatus(tmpData, (byte)0);
                sendData(apdu, tmpData, len);
                return;
            case DO_FINGERPRINTES:
                len = fingerprints.getData(buffer,(byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_CA_FINGERPRINTS:
                len = fingerprintsCA.getData(buffer,(byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_GENERATION_DATES:
                len = dateGeneration.getData(buffer,(byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_SECURITY_SUPPORT_TEMPLATE:
                len = signCount.getTlv(buffer, (byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_SIGNATURE_COUNTER:
                len = signCount.getData(buffer, (byte)0);
                apdu.setOutgoingAndSend((short)0, (short)len);
                return;
            case DO_OPTIONAL1:
                len = optionalData1.getData(tmpData, (byte) 0);
                sendData(apdu, tmpData, len);
                break;
            case DO_OPTIONAL2:
                len = optionalData2.getData(tmpData, (byte) 0);
                sendData(apdu, tmpData, len);
                break;
            case DO_OPTIONAL3:
                if (!chv2.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                len = optionalData3.getData(tmpData, (byte) 0);
                sendData(apdu, tmpData, len);
                break;
            case DO_OPTIONAL4:
                if (!chv3.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                len = optionalData4.getData(tmpData, (byte) 0);
                sendData(apdu, tmpData, len);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private short getCardholderData(byte[] buffer, short offset) {
        short len = offset;

        len = name.getTlv(buffer, len);
        len = langPref.getTlv(buffer, len);
        len = sex.getTlv(buffer, len);
        return len;
    }

    private short getCHVStatus(byte[] buffer, short offset) {
        buffer[offset] = chv ? (byte)1 : (byte)0;
        buffer[(short)(offset + 1)] = CHV1_LENGTH;
        buffer[(short)(offset + 2)] = CHV2_LENGTH;
        buffer[(short)(offset + 3)] = CHV3_LENGTH;
        buffer[(short)(offset + 4)] = chv1.getTriesRemaining();
        buffer[(short)(offset + 5)] = chv2.getTriesRemaining();
        buffer[(short)(offset + 6)] = chv3.getTriesRemaining();
        return (short)(offset + 7);
    }

    private short getDiscData(byte[] buffer, short offset) {
        short len = offset;

        Util.arrayCopy(extendedCap, (short)0, buffer, (short)len, (short) extendedCap.length);
        len += (short) extendedCap.length;
        Util.arrayCopy(algAttrSign, (short)0, buffer, (short)len, (short) algAttrSign.length);
        len += 7;
        Util.arrayCopy(algAttrDec, (short)0, buffer, (short)len, (short) algAttrDec.length);
        len += 7;
        Util.arrayCopy(algAttrAuth, (short)0, buffer, (short)len, (short) algAttrAuth.length);
        len += 7;
        buffer[len++] = (byte)0xc4;
        buffer[len++] = (byte)0x7;
        len = getCHVStatus(buffer, len);
        len = fingerprints.getTlv(buffer, len);
        len = fingerprintsCA.getTlv(buffer, len);
        len = dateGeneration.getTlv(buffer, len);
        return len;
    }
    
    private void putData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short p1p2 = Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]);
        short lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);
        short l;
        short len;

        len = receiveData(apdu, tmpData);
        
        putData(tmpData, len, p1p2);
    }

    private void putData(byte[] data, short dataLen, short p1p2) {
        
        
        if (p1p2 == DO_OPTIONAL1) {
            if (!chv2.isValidated()) {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            } else {
                optionalData1.setData(data, dataLen);
                return;
            }
        }
        if (p1p2 == DO_OPTIONAL3) {
            if (!chv2.isValidated()) {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            } else {
                optionalData3.setData(data, dataLen);
                return;
            }
        }

        if (!chv3.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        switch (p1p2) {
            case DO_NAME:
                name.setData(data, dataLen);
                return;
            case DO_LOGIN_DATA:
                loginData.setData(data, dataLen);
                return;
            case DO_LANGUAGE:
                langPref.setData(data, dataLen);
                return;
            case DO_SEX:
                sex.setData(data, dataLen);
                return;
            case DO_URL:
                url.setData(data, dataLen);
                return;
            case DO_CHV_STATUS:
                if (dataLen != 1) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                if (data[0] > (byte)1 || data[0] < (byte)0) {
                    ISOException.throwIt(ISO7816.SW_DATA_INVALID);
                }
                chv = (data[0] ==  1);
                return;
            case DO_FINGERPRINT_SIGN:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprints.setData(data, (short)0, dataLen);
                return;
            case DO_FINGERPRINT_DEC:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprints.setData(data, (short)20, dataLen);
                return;
            case DO_FINGERPRINT_AUTH:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprints.setData(data, (short)40, dataLen);
                return;
            case DO_CA_FINGERPRINT1:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprintsCA.setData(data, (short)0, dataLen);
                return;
            case DO_CA_FINGERPRINT2:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprintsCA.setData(data, (short)20, dataLen);
                return;
            case DO_CA_FINGERPRINT3:
                if (dataLen != (short)20) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                fingerprints.setData(data, (short)40, dataLen);
                return;
            case DO_GENERATION_DATE_SIGN:
                if (dataLen != (short)4) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                dateGeneration.setData(data, (short)0, dataLen);
                return;
            case DO_GENERATION_DATE_DEC:
                if (dataLen != (short)4) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                dateGeneration.setData(data, (short)4, dataLen);
                return;
            case DO_GENERATION_DATE_AUTH:
                if (dataLen != (short)4) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                dateGeneration.setData(data, (short)8, dataLen);
                return;
            case DO_OPTIONAL2:
                optionalData2.setData(data, dataLen);
                return;
            case DO_OPTIONAL4:
                optionalData4.setData(data, dataLen);
                return;
            case DO_KEY_IMPORT:
                importKey(data, (short) 0, dataLen);
            default:
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }
    
    private void putDataChaining(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();
        
        Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, tmpData, incomingDataOffset, dataLen);
        incomingDataOffset += (short) (dataLen - ISO7816.OFFSET_CDATA);
        if ((buffer[ISO7816.OFFSET_CLA] & CLA_CHAINING) == CLA_CHAINING) {
            // First chaining command should arive when incomingDataOffset == 0 (BUGBUG: should be checked against expected CLA/INS)
            // Second... chainig command will have incomingDataOffset > 0
            
            // Wait for next chaining command
        }
        else {
            // Either last command in chain or simple command
            // Execute PUT DATA
            short p1p2 = Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]);
            putData(tmpData, incomingDataOffset, p1p2);
        }
    }
    

    private void generateAssymetricKeyPair(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);
        byte p1 = buffer[ISO7816.OFFSET_P1];
        short len;
        short crt;

        if (buffer[ISO7816.OFFSET_P2] != (byte)0) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
        len = apdu.setIncomingAndReceive();
        if (lc != (short)2 || lc != len) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        crt = Util.getShort(buffer, ISO7816.OFFSET_CDATA);
        switch (p1) {
            case (byte)0x80:
                if (!chv3.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                switch (crt) {
                    case (short)0xb600:
                        keySign.genKeyPair();
                        Util.arrayFillNonAtomic(signCount.getData(), (short)0, (short)3, (byte)0);
                        break;
                    case (short)0xb800:
                        keyDecrypt.genKeyPair();
                        break;
                    case (short)0xa400:
                        keyAuth.genKeyPair();
                        break;
                    default:
                        ISOException.throwIt(ISO7816.SW_DATA_INVALID);
                }
                sendPublicKey(crt, apdu);
                return;
            case (byte)0x81:
                sendPublicKey(crt, apdu);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private void sendPublicKey(short crt, APDU apdu) {
        RSAPublicKey key = null;
        short len, l;

        switch(crt) {
            case (short)0xb600:
                key = (RSAPublicKey)keySign.getPublic();
                break;
            case (short)0xb800:
                key = (RSAPublicKey)keyDecrypt.getPublic();
                break;
            case (short)0xa400:
                key = (RSAPublicKey)keyAuth.getPublic();
                break;
            default:
                ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }
        tmpData[0] = (byte)0x7f;
        tmpData[1] = (byte)0x49;
        tmpData[2] = (byte)0x82;
        tmpData[5] = (byte)0x81;
        len = 6;
//        len = 5;
        l = key.getModulus(tmpData, (short)(len + 3));
        tmpData[len++] = (byte)0x82;
        Util.setShort(tmpData, len, l);
        len += 2;
//        tmpData[len++] = (byte)l;
        len += l;
        tmpData[len++] = (byte)0x82;
        l = key.getExponent(tmpData, (short)(len + 1));
        tmpData[len++] = (byte)l;
        len += l;
        Util.setShort(tmpData, (short)3, (short)(len - 5));
//        tmpData[3] = (byte)(len - 4);
        sendData(apdu, tmpData, len);
    }

    private void performSecurityOperation(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short p1p2 = Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]);
        short len;

        switch (p1p2) {
            case (short)0x9e9a:
                byte[] counter = signCount.getData();
                if (!chv1.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                if (!chv) { chv1.reset(); }
                len = receiveData(apdu, tmpData);
                sig.init(keySign.getPrivate(), Cipher.MODE_ENCRYPT);
                len = sig.doFinal(tmpData, (short)0, len, tmpData, (short)0);
                if (counter[2] < 255) {
                    counter[2]++;
                } else {
                    counter[2] = 0;
                    if (counter[1] < 255) {
                        counter[1]++;
                    } else {
                        counter[1] = 0;
                        counter[0]++;
                    }
                }
                sendData(apdu, tmpData, len);
                return;
            case (short)0x8086:
                if (!chv2.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                len = receiveData(apdu, tmpData);
                cipher.init(keyDecrypt.getPrivate(), Cipher.MODE_DECRYPT);
                len = cipher.doFinal(tmpData, (short)1, (short)(len - 1), tmpData, (short)0);
                sendData(apdu, tmpData, len);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private void changeResetChv(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        boolean reset = false;

        if (buffer[ISO7816.OFFSET_INS] == RESET_RETRY_COUNTER) {
            if (buffer[ISO7816.OFFSET_P1] != (byte)02 ||
                        buffer[ISO7816.OFFSET_P2] < (byte)0x81 ||
                        buffer[ISO7816.OFFSET_P2] > (byte)0x82) {
                    ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
            }
            if (chv3.isValidated()) {
                reset = true;
            } else {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            }
        } else if (buffer[ISO7816.OFFSET_P1] != (byte)01 ||
                buffer[ISO7816.OFFSET_P2] < (byte)0x81 ||
                buffer[ISO7816.OFFSET_P2] > (byte)0x83) {
                    ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

        byte p2 = buffer[ISO7816.OFFSET_P2];
        short len = receiveData(apdu, tmpData);
        switch (p2) {
            case (byte)0x81:
                if (!reset && !chv1.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                } else if (len >= (short)6 && len <= CHV1_LENGTH) {
                    chv1.update(tmpData, (short)0, (byte)len);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }
                return;
            case (byte)0x82:
                if (!reset && !chv2.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                } else if (len >= (short)6 && len <= CHV2_LENGTH) {
                    chv2.update(tmpData, (short)0, (byte)len);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }
                return;
            case (byte)0x83:
                if (!reset && !chv3.isValidated()) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                } else if (len >= (short)8 && len <= CHV3_LENGTH) {
                    chv3.update(tmpData, (short)0, (byte)len);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }

    /**
     * Sends data to the terminal. The data to be sent must begin at index 0 in
     * the array specified by the buffer argument.
     * If the number of bytes to send as specified by the len parameter is
     * greater the 254, this method will automatically generate 61xx status
     * bytes in the response to indicate more data available by GET_RESPONSE.
     * If the length is such as 61xx status is generated the data to be sent
     * MUST be located in tmpData, and tmpData MUST NOT be modified until all
     * data has been fetched or a command other the GET_RESPONSE is issued from
     * the terminal.
     * Note that if the length of the data is more then 254 bytes an
     * ISOException will be thrown so no code will be executed after the call
     * to sendData().
     * @param apdu the current APDU object as received in process.
     * @param buffer the buffer containing the data to be sent.
     * @param len the number of bytes to be sent.
     */
///* ORIGINAL VERSION
    private void sendData(APDU apdu, byte[] buffer, short len) {
        apdu.setOutgoing();
       if (len > (short)254) {
            apdu.setOutgoingLength((short)254);
            apdu.sendBytesLong(buffer, remainingDataOffset, (short)254);
            remainingDataOffset += 254;
            remainingDataLength = (short)(len - 254);
            ISOException.throwIt((short)(SW_MORE_DATA + ((remainingDataLength > 254) ? 254 : remainingDataLength)));
        } else {
            apdu.setOutgoingLength(len);
            apdu.sendBytesLong(buffer, remainingDataOffset, len);
        }
    }
/**/
/*
    private void sendData(APDU apdu, byte[] buffer, short len) {
      apdu.setOutgoing();
       if (len > (short)254) {
            Util.arrayCopyNonAtomic(buffer, remainingDataOffset, apdu.getBuffer(), (short) 0, (short) 254);
            remainingDataOffset += 254;
            remainingDataLength = (short)(len - 254);
            apdu.setOutgoingAndSend((short) 0, (short)254);
            ISOException.throwIt((short)(SW_MORE_DATA + ((remainingDataLength > 254) ? 254 : remainingDataLength)));
        } else {
          byte[] buf = apdu.getBuffer();
          remainingDataOffset = 0;
          Util.arrayCopyNonAtomic(buffer, remainingDataOffset, buf, (short) 0, len);
          //apdu.setOutgoingAndSend((short) 0, len);
          short le = apdu.setOutgoing();
          if (le < (short)2) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );

          apdu.setOutgoingLength(len);
          apdu.sendBytes((short) 0, len);
        }
    }
/**/
/*
    private void sendData(APDU apdu, byte[] buffer, short len) {
      byte[] bufferOut = apdu.getBuffer();
      Util.arrayCopyNonAtomic(bufferOut, (short) 0, buffer, (short) 0, len);
      apdu.setOutgoingAndSend((short) 0, len);
      ISOException.throwIt((short)(SW_SUCCESS));
   }
   /**/
    private short receiveData(APDU apdu, byte[] data) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);
        short l;
        short len;

        if (lc > 254) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        l = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, (short)ISO7816.OFFSET_CDATA, data, (short)0, l);
        lc -= l;
        len = l;
        while (lc > 0) {
            l = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
            Util.arrayCopy(buffer, (short)ISO7816.OFFSET_CDATA, data, len, l);
            lc -= l;
            len += l;
        }
        return len;
    }
    
    private short parseTLVLength(byte[] data, short dataOffset, short[] result) {
        if (data[dataOffset] == (byte) 0x81) {
            result[0] = data[(short) (dataOffset + 1)];
            return (short) (dataOffset + 2);
        }
        else {
            if (data[dataOffset] == (byte) 0x82) {
                result[0] = Util.makeShort(data[(short) (dataOffset + 1)], data[(short)(dataOffset + 2)]);
                return (short) (dataOffset + 3);
            }
            else { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 2)); }
        }     
        return dataOffset;
    }
    private void importKey(byte[] data, short dataOffset, short dataLen) {
/*
4d           82 01 76           b6 00      7f 48 08     91 64            92 81 80 93 81 80 5f 48      82 01 64         
| exthdrlist |datalength=0x0176 |sign key  | privktempl |exponentlen 64B |p len   |q len   |priv key  | tag total length=0x0164
 
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 {Public exponent: e, length 0x64B}
dc 0b 08 85 ac a0 20 a0 cb 3c 26 00 bc 0b 40 a1 0c 04 77 1f 00 74 91 24 2b 99 c8 e2 a0 10 d7 50 5a 75 9a 47 90 1b 44 79 ca 5b b5 20 df 9a 48 d4 16 b2 3c 8f 06 03 68 f0 45 1d 3e 1c 5d 22 b7 c1 ba f5 55 f4 23 30 30 76 08 99 c6 64 5f a7 31 85 dd 4f b8 1b 5d 20 13 6d c1 69 fb 52 2a 19 53 a9 c4 cc 0b e7 84 42 fb 61 f7 b2 f7 69 45 ae 51 ba be 9d 80 01 73 af dc 28 fd 87 87 64 db 28 83 a5 {Prime1: p, length 0x80 == 128B}
ec 81 7e da e8 ae 29 4d 0b c6 06 d5 56 9b 06 72 cc 29 16 7d 3d 94 68 1f 2c f4 af 72 69 a9 96 9b 2b e0 48 da d8 ae c9 a9 a6 08 b2 37 27 07 7f e4 86 ed 40 9e 38 6d a3 69 91 e9 03 fc 3d 64 ad eb df 65 07 a8 6e 9f 2e 8e 44 cc 5c b5 01 a0 17 71 55 f6 37 e7 b1 4f 79 ed e4 b0 a9 7c 46 7a 48 00 7a eb 8e 7e 97 b6 74 47 a5 69 4f 41 2a 58 ec b6 73 d2 d2 43 69 88 bb d2 22 85 1c 89 96 46 8e e5 {Prime2: q, length 0x80 == 128B} 
 */        
        if (data[dataOffset] != 0x4d) { ISOException.throwIt( ISO7816.SW_WRONG_DATA); } // check extended header list tag
        dataOffset++;
        short dataLength = 0;
        if (data[dataOffset] == (byte) 0x81) {
            dataLength = data[(short) (dataOffset + 1)];
            dataOffset += 2;
        }
        else {
            if (data[dataOffset] == (byte) 0x82) {
                dataLength = Util.makeShort(data[(short) (dataOffset + 1)], data[(short)(dataOffset + 2)]);
                dataOffset += 3;
            }
            else ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 2));
        }
        short keyType = Util.makeShort(data[dataOffset], data[(short)(dataOffset + 1)]);
        dataOffset += 2;
        if (data[dataOffset] != (byte) 0x7f || data[(short) (dataOffset + 1)] != (byte) 0x48) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 3)); } // check private key template tag
        dataOffset += 2;
        if (data[dataOffset] != (byte) 0x08) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 4)); } // check expected length
        dataOffset++; 
        if (data[dataOffset] != (byte) 0x91) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 5)); } // check exponent length tag
        dataOffset++; 
        short exponentLength = data[dataOffset];
        dataOffset++; 
        if (data[dataOffset] != (byte) 0x92) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 6)); } // check prime P length tag
        dataOffset++; 
        short primePLength = 0;
        if (data[dataOffset] == (byte) 0x81) {
            primePLength = data[(short) (dataOffset + 1)];
            dataOffset += 2;
        }
        else {
            if (data[dataOffset] == (byte) 0x82) {
                primePLength = Util.makeShort(data[(short) (dataOffset + 1)], data[(short)(dataOffset + 2)]);
                dataOffset += 3;
            }
            else ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 7));
        }
        if (data[dataOffset] != (byte) 0x93) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 8)); } // check prime Q length tag
        dataOffset++; 
        short primeQLength = 0;
        if (data[dataOffset] == (byte) 0x81) {
            primeQLength = data[(short) (dataOffset + 1)];
            dataOffset += 2;
        }
        else {
            if (data[dataOffset] == (byte) 0x82) {
                primeQLength = Util.makeShort(data[(short) (dataOffset + 1)], data[(short)(dataOffset + 2)]);
                dataOffset += 3;
            }
            else ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 9));
        }
        if (data[dataOffset] != 0x5f || data[(short) (dataOffset + 1)] != 0x48) { ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 0x0a)); } // check private key tag
        dataOffset += 2;
        
        if (data[dataOffset] == (byte) 0x81) {
            dataLength = data[(short) (dataOffset + 1)];
            dataOffset += 2;
        }
        else {
            if (data[dataOffset] == (byte) 0x82) {
                dataLength = Util.makeShort(data[(short) (dataOffset + 1)], data[(short)(dataOffset + 2)]);
                dataOffset += 3;
            }
            else ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 0x0b));
        }
        
        if (primePLength != primeQLength) {ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA + 0x0c));}
/*        
        RSAPrivateCrtKey privKey = null;
        RSAPublicKey pubKey = null;
        switch(keyType) {
            case (short)0xb600:
                privKey = (RSAPrivateCrtKey)keySign.getPrivate();
                pubKey = (RSAPublicKey)keySign.getPublic();
                break;
            case (short)0xb800:
                privKey = (RSAPrivateCrtKey)keyDecrypt.getPrivate();
                pubKey = (RSAPublicKey)keySign.getPublic();
                break;
            case (short)0xa400:
                privKey = (RSAPrivateCrtKey)keyAuth.getPrivate();
                pubKey = (RSAPublicKey)keySign.getPublic();
                break;
            default:
                ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }        
       
        // Set exponent
        pubKey.setExponent(data, dataOffset, exponentLength);
        dataOffset += exponentLength;
        
        // Set prime P
        privKey.setP(data, dataOffset, primePLength);
        dataOffset += exponentLength;
        
        // Set prime Q
        privKey.setQ(data, dataOffset, primeQLength);
        dataOffset += exponentLength;
*/        
    }    
/*
    private void exportKeyPair(APDU apdu) {
        // Help export function for private key!!!
        // TODO: export under secure channel to other smart card only
        // Allow to export key only if signature count is lower then 6 (only key generation was performed, 
        // not any other real signing yet)

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short  crt = Util.makeShort(buffer[ISO7816.OFFSET_CDATA], buffer[ISO7816.OFFSET_CDATA + 1]);
        short signCounter = Util.makeShort(signCount.data[0], signCount.data[1]);

        if (crt == 0x5555) bNotExported = 0;
        else {
            if ((signCounter < (short) 6) && (bNotExported == 0x55)) {
                RSAPrivateCrtKey privKey = null;
                RSAPublicKey pubKey = null;
                short len = 0;
                short l = 0;

                switch(crt) {
                    case (short)0xb600:
                        privKey = (RSAPrivateCrtKey)keySign.getPrivate();
                        pubKey = (RSAPublicKey)keySign.getPublic();
                        break;
                    case (short)0xb800:
                        privKey = (RSAPrivateCrtKey)keyDecrypt.getPrivate();
                        pubKey = (RSAPublicKey)keySign.getPublic();
                        break;
                    case (short)0xa400:
                        privKey = (RSAPrivateCrtKey)keyAuth.getPrivate();
                        pubKey = (RSAPublicKey)keySign.getPublic();
                        break;
                    default:
                        ISOException.throwIt(ISO7816.SW_DATA_INVALID);
                }

                len = 0;
                if (buffer[ISO7816.OFFSET_P2] == 0x55) {
                    // Export private key
                    switch (buffer[ISO7816.OFFSET_P1]) {
                        case 1:
                            l = privKey.getDP1(tmpData, (short)(len + 3));
                            break;
                        case 2:
                            l = privKey.getDQ1(tmpData, (short)(len + 3));
                            break;
                        case 3:
                            l = privKey.getP(tmpData, (short)(len + 3));
                            break;
                        case 4:
                            l = privKey.getPQ(tmpData, (short)(len + 3));
                            break;
                        case 5:
                            l = privKey.getQ(tmpData, (short)(len + 3));
                            break;
                        default:
                            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
                    }
                    tmpData[len++] = (byte)0x82;
                    Util.setShort(tmpData, len, l);
                    len += 2;
                    len += l;
                }
                else {
                    // Export public key
                    l = pubKey.getModulus(tmpData, (short)(len + 3));
                    tmpData[len++] = (byte)0x82;
                    Util.setShort(tmpData, len, l);
                    len += 2;
                    len += l;
                    tmpData[len++] = (byte)0x82;
                    l = pubKey.getExponent(tmpData, (short)(len + 1));
                    Util.setShort(tmpData, len, l);
                    len += 2;
                    len += l;
                }

                sendData(apdu, tmpData, len);
            }
        }
    }
 /**/
    
    /**
     * Determine if APDU was sent via RFID interface
     * Returns true for contactless protocols
     */
    private boolean usingRF(APDU apdu)
    {
	short media=(short)(APDU.getProtocol() & (short)APDU.PROTOCOL_MEDIA_MASK);
	return (media==APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_A) ||
		(media==APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_B);
    }

    /**
     * Read back user policy
     * returns:
     *  short: total number of possible keys
     *  short: transfer buffer size
     *  byte:  use separate auth over RFID
     *  byte:  use OOB auth
     */
    private void ReadPolicy(APDU apdu)
    {
        byte[]	apdubuf = apdu.getBuffer();

        short	dataLen = apdu.setIncomingAndReceive();
        short   offset = (short)0;

        Util.arrayFillNonAtomic(apdubuf, offset, (short) 240, (byte) 0);

	Util.setShort(apdubuf, offset, m_maxKeys);
	offset = (short)(offset + 2);
	Util.setShort(apdubuf, offset, m_bSize);
	offset = (short)(offset + 2);
	apdubuf[offset] = m_separateAuth?(byte)1:(byte)0;
	++offset;
	apdubuf[offset] = m_oobAuth;
	++offset;

	apdu.setOutgoingAndSend((byte) 0, offset);
    }

    private void KeyPush(APDU apdu)
    {
	if(usingRF(apdu)) { // Just for testing
	    ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED ) ;
    }
    
}