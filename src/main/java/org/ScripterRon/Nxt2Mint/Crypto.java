/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Mint;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic functions using Curve25519
 *
 * Based on the Nxt reference software (NRS)
 */
public class Crypto {

    /** Instance of a SHA-256 digest which we will use as needed */
    private static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);      // Never happen
        }
    }

    /**
     * Calculate the SHA-256 hash of a string
     *
     * @param       input           Data to be hashed
     * @return                      The hash digest
     */
    public static byte[] singleDigest(String input) {
        byte[] bytes;
        try {
            bytes = singleDigest(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exc) {
            bytes = null;
        }
        return bytes;
    }

    /**
     * Calculate the SHA-256 hash of a byte array
     *
     * @param       input           Data to be hashed
     * @return                      The hash digest
     */
    public static byte[] singleDigest(byte[] input) {
        byte[] bytes;
        synchronized (digest) {
            digest.reset();
            bytes = digest.digest(input);
        }
        return bytes;
    }

    /**
     * Calculate the hash of two byte arrays
     *
     * @param       input1          First byte array
     * @param       input2          Second byte array
     * @return                      The hash digest
     */
    public static byte[] singleDigest(byte[] input1, byte[] input2) {
        byte[] bytes;
        synchronized (digest) {
            digest.reset();
            digest.update(input1);
            bytes = digest.digest(input2);
        }
        return bytes;
    }

    /**
     * Return the public key for the supplied secret phrase
     *
     * @param       secretPhrase        Account secret phrase
     * @return                          Public key
     * @throws      KeyException        Public key is not canonical
     */
    public static byte[] getPublicKey(String secretPhrase) throws KeyException {
        byte[] publicKey = new byte[32];
        Curve25519.keygen(publicKey, null, singleDigest(secretPhrase));
        if (!Curve25519.isCanonicalPublicKey(publicKey))
            throw new KeyException("Public key is not canonical");
        return publicKey;
    }

    /**
     * Sign a message
     *
     * @param       message             The message to be signed
     * @param       secretPhrase        Private key phrase
     * @return                          The signed message
     * @throws      KeyException        Unable to sign message
     */
    public static byte[] sign(byte[] message, String secretPhrase) throws KeyException {
        byte[] signature = new byte[64];
        try {
            synchronized(digest) {
                digest.reset();
                byte[] P = new byte[32];
                byte[] s = new byte[32];
                Curve25519.keygen(P, s, digest.digest(secretPhrase.getBytes("UTF-8")));

                byte[] m = digest.digest(message);

                digest.update(m);
                byte[] x = digest.digest(s);

                byte[] Y = new byte[32];
                Curve25519.keygen(Y, null, x);

                digest.update(m);
                byte[] h = digest.digest(Y);

                byte[] v = new byte[32];
                Curve25519.sign(v, h, x, s);

                System.arraycopy(v, 0, signature, 0, 32);
                System.arraycopy(h, 0, signature, 32, 32);

                if (!Curve25519.isCanonicalSignature(signature)) {
                    throw new KeyException("Signature is not canonical");
                }
            }
        } catch (RuntimeException|UnsupportedEncodingException e) {
            // Never happen
        }
        return signature;
    }
}
