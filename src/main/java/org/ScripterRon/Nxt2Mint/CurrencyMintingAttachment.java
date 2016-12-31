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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CurrencyMint attachment
 */
public class CurrencyMintingAttachment {

    /** Nonce */
    private final long nonce;

    /** Currency identifier */
    private final long currencyId;

    /** Minting units */
    private final long units;

    /** Minting counter */
    private final long counter;

    /**
     * Create a new currency minting attachment
     *
     * @param   response                Response to CurrencyMint
     * @throws  IdentifierException     Invalid identifier
     * @throws  NumberFormatException   Invalid numeric value
     */
    public CurrencyMintingAttachment(Response response) throws IdentifierException, NumberFormatException {
        this.nonce = response.getLong("nonce");
        this.currencyId = response.getId("currency");
        this.units = response.getLong("units");
        this.counter = response.getLong("counter");
    }

    /**
     * Create a new currency minting attachment
     *
     * @param   transactionBytes            Transaction bytes
     * @throws  BufferUnderflowException    End-of-data reached parsing attachment
     * @throws  IllegalArgumentException    Invalid attachment
     */
    public CurrencyMintingAttachment(byte[] transactionBytes)
                                            throws BufferUnderflowException, IllegalArgumentException {
        ByteBuffer buffer = ByteBuffer.wrap(transactionBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(Transaction.BASE_LENGTH);
        int version = buffer.get();
        if (version != 1)
            throw new IllegalArgumentException("Attachment version is not 1");
        this.nonce = buffer.getLong();
        this.currencyId = buffer.getLong();
        this.units = buffer.getLong();
        this.counter = buffer.getLong();
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
     * Return the nonce
     *
     * @return                      Nonce
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Return the minting units
     *
     * @return                      Minting units
     */
    public long getUnits() {
        return units;
    }

    /**
     * Return the minting counter
     *
     * @return                      Minting counter
     */
    public long getCounter() {
        return counter;
    }
}
