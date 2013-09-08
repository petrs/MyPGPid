/*
 * MyPGPid.java - Java card implementation of openpgp card
 * Driver for:
 *  NXP JCOP 41 v2.2.1 72K
 *  NXP JCOP 21 v2.2.1 72K
 *  Oberthur Cosmo Dual 72K
 *
 * Copyright (C) 2013 Diego 'NdK' Zuccato, (C) 2013 Petr Svenda (MyPGPid project)
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

package MyPGPid;
import javacard.security.*;

class cardJCOP extends MyPGPid {
    public static short BuffSize() {return (short)0x300;};
    public static byte UserConsent() {return (byte)0; };
    public static byte DEF_ALG() { return KeyPair.ALG_RSA_CRT; };
    public static short DEF_KEY_LEN() { return (short) 2048; };
    public static byte DEF_CAKEYTYPE() {return KeyBuilder.TYPE_RSA_PUBLIC; };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new cardJCOP(bArray, bOffset, bLength);
    }

    protected cardJCOP(byte[] bArray, short bOffset, byte bLength) {
	super(bArray, bOffset, bLength);
    }
}
