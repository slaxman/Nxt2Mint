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
import static org.ScripterRon.Nxt2Mint.Main.log;

/**
 * Currency minting hash functions using the CPU
 */
public abstract class HashFunction {

    /** Hash count */
    protected int hashCount;

    /** Nonce */
    protected long nonce;

    /** Hash digest */
    protected final byte[] digest = new byte[32];

    /**
     * Private constructor for use by subclasses
     */
    protected HashFunction() {

    }

    /**
     * Create a hash function for the specified algorithm
     *
     * @param       algorithm       Hash algorithm
     * @return                      Hash function
     */
    public static HashFunction factory(int algorithm) {
        HashFunction hashFunction;
        switch (algorithm) {
            case 2:                 // SHA256
                hashFunction = new HashSha256();
                break;
            case 3:                 // SHA3
                hashFunction = new HashSha3();
                break;
            case 5:                 // SCRYPT
                hashFunction = new HashScrypt();
                break;
            case 25:                // KECCAK25
                hashFunction = new HashKnv25();
                break;
            default:
                throw new IllegalArgumentException("CPU hash algorithm "+algorithm+" is not supported");
        }
        return hashFunction;
    }

    /**
     * Check for a supported algorithm
     *
     * @param       algorithm       Hash algorithm
     * @return                      TRUE if the algorithm is supported
     */
    public static boolean isSupported(int algorithm) {
        return (algorithm==2 || algorithm==3 || algorithm==5 || algorithm==25);
    }

    /**
     * Hash the input bytes
     *
     * @param       input           Input bytes
     * @param       target          Target bytes
     * @param       nonce           Initial nonce
     * @return                      TRUE if the target was met
     */
    public abstract boolean hash(byte[] input, byte[] target, long nonce);

    /**
     * Return the nonce used to solve the hash
     *
     * @return                      Nonce
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Return the execution count
     *
     * @return                      Kernel execution count
     */
    public int getCount() {
        return hashCount;
    }
}
