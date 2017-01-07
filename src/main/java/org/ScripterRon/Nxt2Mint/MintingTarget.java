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

import org.ScripterRon.Nxt2API.IdentifierException;
import org.ScripterRon.Nxt2API.Response;

import java.math.BigInteger;

/**
 * The minting target used to mint a coin in the Nxt Monetary System
 */
public class MintingTarget {

    /** Currency identifier */
    private final long currencyId;

    /** The minting difficulty */
    private final BigInteger difficulty;

    /** Hash target */
    private final byte[] target;

    /** Mint counter */
    private long counter;

    /**
     * Create the minting target from the response for 'getMintingTarget'
     *
     * @param       response                Response for getAccount request
     * @throws      IdentifierException     Invalid account identifier
     * @throws      NumberFormatException   Invalid numeric string
     */
    public MintingTarget(Response response) throws IdentifierException, NumberFormatException {
        this.currencyId = response.getId("currency");
        this.difficulty = new BigInteger(response.getString("difficulty"));
        this.target = response.getHexString("targetBytes");
        this.counter = response.getLong("counter");
    }

    /**
     * Return the currency identifier
     *
     * @return                      Currency identifier
     */
    public long getCurrencyId() {
        return currencyId;
    }

    /**
     * Return the target difficulty
     *
     * @return                      Target difficulty
     */
    public BigInteger getDifficulty() {
        return difficulty;
    }

    /**
     * Return the hash target
     *
     * @return                      Hash target
     */
    public byte[] getTarget() {
        return target;
    }

    /**
     * Return the minting counter
     *
     * @return                      Minting counter
     */
    public long getCounter() {
        return counter;
    }

    /**
     * Set the minting counter
     *
     * @param       counter         Minting counter
     */
    public void setCounter(long counter) {
        this.counter = counter;
    }

    /**
     * Return the hash code
     *
     * @return                      Hash code
     */
    @Override
    public int hashCode() {
        return (int)currencyId;
    }

    /**
     * Compares two minting targets
     *
     * @param       obj             Currency to compare
     * @return                      TRUE if the minting target is equal to this target
     */
    @Override
    public boolean equals(Object obj) {
        return (obj != null && (obj instanceof MintingTarget) && currencyId==((MintingTarget)obj).currencyId);
    }
}
