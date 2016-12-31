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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Wallet transaction
 */
public class Transaction {

    /** Transaction length */
    public static final int BASE_LENGTH = 149;

    /** Signature offset in the transaction bytes */
    public static final int SIGNATURE_OFFSET = 69;

    /** Transaction version */
    private final int version;

    /** Transaction identifier */
    private final long id;

    /** Transaction full hash */
    private final byte[] fullHash;

    /** Transaction amount */
    private final long amount;

    /** Transaction fee */
    private final long fee;

    /** Transaction timestamp */
    private final Date timestamp;

    /** Transaction sender */
    private final long senderId;

    /** Transaction recipient */
    private final long recipientId;

    /** Transaction type */
    private final String transactionType;

    /** Transaction chain identifier */
    private final int chainId;

    /** Block identifier */
    private long blockId;

    /** Transaction height */
    private int height;

    /**
     * Process a transaction list
     *
     * @param   transactionList         JSON transaction list
     * @return                          Transaction list
     * @throws  NumberFormatException   Invalid numeric value
     */
    public static List<Transaction> processTransactions(List<Map<String, Object>> transactionList)
                                        throws NumberFormatException {
        List<Transaction> txList = new ArrayList<>(transactionList.size());
        transactionList.forEach(txMap -> txList.add(new Transaction(new Response(txMap))));
        return txList;
    }

    /**
     * Create a new wallet transaction
     *
     * @param   response                Nxt transaction response
     * @throws  NumberFormatException   Invalid numeric value
     */
    public Transaction(Response response) throws NumberFormatException {
        this.version = response.getByte("version");
        this.id = response.getLong("transaction");
        this.fullHash = Utils.parseHexString(response.getString("fullHash"));
        this.amount = response.getLong("amountNQT");
        this.fee = response.getLong("feeNQT");
        this.timestamp = new Date((response.getLong("timestamp")) * 1000L + Main.epochBeginning);
        this.senderId = response.getLong("sender");
        this.recipientId = response.getLong("recipient");
        this.chainId = response.getInt("chain");
        int txHeight = response.getInt("height");
        if (txHeight == 0 || txHeight == Integer.MAX_VALUE) {
            this.height = 0;
            this.blockId = 0;
        } else {
            this.height = txHeight;
            this.blockId = response.getLong("block");
        }
        //
        // Get the transaction type
        //
        Map<Integer, String> typeMap = Main.transactionTypes.get(response.getInt("type"));
        if (typeMap != null) {
            String subtype = typeMap.get(response.getInt("subtype"));
            if (subtype != null) {
                this.transactionType = subtype;
            } else {
                this.transactionType = "Unknown";
            }
        } else {
            this.transactionType = "Unknown";
        }
    }

    /**
     * Create a new wallet transaction
     *
     * @param   transactionBytes            Transaction bytes
     * @throws  BufferUnderflowException    Transaction bytes too short
     */
    public Transaction(byte[] transactionBytes) throws BufferUnderflowException {
        ByteBuffer buffer = ByteBuffer.wrap(transactionBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.chainId = buffer.getInt();
        int type = buffer.get();
        int subtype = buffer.get();
        this.version = buffer.get();
        this.timestamp = new Date(((long)buffer.getInt() * 1000L) + Main.epochBeginning);
        buffer.position(buffer.position() + 2);     // Skip deadline
        byte[] publicKey = new byte[32];
        buffer.get(publicKey);
        this.senderId = Utils.getAccountId(publicKey);
        this.recipientId = buffer.getLong();
        this.amount = buffer.getLong();
        this.fee = buffer.getLong();
        byte[] signature = new byte[64];
        buffer.get(signature);
        //
        // Get the transaction type
        //
        Map<Integer, String> typeMap = Main.transactionTypes.get(type);
        if (typeMap != null) {
            String string = typeMap.get(subtype);
            if (string != null) {
                this.transactionType = string;
            } else {
                this.transactionType = "Unknown";
            }
        } else {
            this.transactionType = "Unknown";
        }
        this.height = 0;
        this.blockId = 0;
        //
        // Calculate the transaction identifier
        //
        byte[] zeroSignature = Arrays.copyOf(transactionBytes, transactionBytes.length);
        Arrays.fill(zeroSignature, SIGNATURE_OFFSET, SIGNATURE_OFFSET+64, (byte)0);
        byte[] signatureHash = Crypto.singleDigest(signature);
        this.fullHash = Crypto.singleDigest(zeroSignature, signatureHash);
        this.id = Utils.fullHashToId(this.fullHash);
    }

    /**
     * Get the transaction type
     *
     * @return                      Transaction type
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Get the chain identifier
     *
     * @return                      Chain identifier
     */
    public int getChainId() {
        return chainId;
    }

    /**
     * Get the transaction version
     *
     * @return                      Transaction version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the transaction identifier
     *
     * @return                      Transaction identifier
     */
    public long getId() {
        return id;
    }

    /**
     * Get the transaction full hash
     *
     * @return                      Full hash
     */
    public byte[] getFullHash() {
        return fullHash;
    }

    /**
     * Get the sender
     *
     * @return                      Sender
     */
    public long getSenderId() {
        return senderId;
    }

    /**
     * Get the recipient
     *
     * @return                      Recipient or zero if no recipient
     */
    public long getRecipientId() {
        return recipientId;
    }

    /**
     * Get the transaction amount
     *
     * @return                      Amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Get the transaction fee
     *
     * @return                      Fee
     */
    public long getFee() {
        return fee;
    }

    /**
     * Get the transaction timestamp
     *
     * @return                      Timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Get the block identifier
     *
     * @return                      Block identifier (0 returned if unconfirmed)
     */
    public long getBlockId() {
        return blockId;
    }

    /**
     * Set the block identifier
     *
     * @param   blockId             New block identifier
     */
    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    /**
     * Get the transaction height
     *
     * @return                      Height (Integer.MAX_VALUE returned if unconfirmed)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the transaction height
     *
     * @param   height              New transaction height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Get the hash code
     *
     * @return                      Hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(fullHash);
    }

    /**
     * Check if two transactions are equal
     *
     * @param   obj                 Comparison transaction
     * @return                      TRUE if the transactions are the same
     */
    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof Transaction) && Arrays.equals(fullHash, ((Transaction)obj).fullHash));
    }
}
