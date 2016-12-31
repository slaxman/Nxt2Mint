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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Mint mints coins for a Nxt Monetary System currency using multiple worker
 * threads to perform the hash functions.
 */
public class Mint {

    /** Mint thread */
    private static Thread mintThread;

    /** Thread list */
    private static final List<MintWorker> workers = new ArrayList<>();

    /** Thread group */
    private static final ThreadGroup threadGroup = new ThreadGroup("Workers");

    /** Solution queue */
    private static final ArrayBlockingQueue<Solution> solutions = new ArrayBlockingQueue<>(10);

    /** Pending queue */
    private static final List<Solution> pending = new LinkedList<>();

    /** Block height at last solution submission */
    private static int submitHeight;

    /** Counter at last solution submission */
    private static long submitCounter;

    /** Current solution counter */
    private static long counter;

    /** Current minting target */
    public static MintingTarget mintingTarget;

    /**
     * Start minting
     */
    public static void mint() {
        Response response;
        mintThread = Thread.currentThread();
        //
        // Get the initial currency counter.  We will increment this counter for
        // each minting transaction.  If the account has unconfirmed transactions, we
        // need to skip the current counter since the server does not increment the counter until
        // the transaction is confirmed in a block.
        //
        counter = Main.mintingTarget.getCounter();
        try {
            response = Request.getUnconfirmedTransactions(Main.chainId, Main.accountId);
            if (!response.getObjectList("unconfirmedTransactions").isEmpty())
                counter++;
        } catch (IOException exc) {
            log.error("Unable to get unconfirmed transactions", exc);
        }
        try {
            //
            // Start the CPU worker threads
            //
            for (int i=0; i<Main.cpuThreads; i++) {
                MintWorker worker = new MintWorker(i, solutions, false, 0);
                Thread thread = new Thread(threadGroup, worker);
                thread.start();
                workers.add(worker);
            }
            //
            // Start the GPU worker threads
            //
            if (Main.gpuIntensity > 0) {
                for (Integer gpuId : Main.gpuDevices) {
                    MintWorker worker = new MintWorker(workers.size(), solutions, true, gpuId);
                    Thread thread = new Thread(threadGroup, worker);
                    thread.start();
                    workers.add(worker);
                }
            }
            //
            // Mint coins until shutdown
            //
            boolean workDispatched = false;
            while (true) {
                if (mintThread.isInterrupted())
                    throw new InterruptedException("Shutting down");
                //
                // Process completed solutions
                //
                if (workDispatched && (pending.isEmpty() || !solutions.isEmpty())) {
                    Solution solution = solutions.take();
                    if (solution.getCounter() > submitCounter) {
                        workDispatched = false;
                        submitCounter = solution.getCounter();
                        pending.add(solution);
                        log.debug(String.format("Solution for counter %d added to pending queue", solution.getCounter()));
                    }
                }
                //
                // Dispatch the new target if the workers are idle
                //
                if (!workDispatched) {
                    try {
                        response = Request.getMintingTarget(Main.currencyId, Main.accountId, Main.mintingUnits);
                        mintingTarget = new MintingTarget(response);
                        mintingTarget.setCounter(counter);
                        counter++;
                    } catch (IOException exc) {
                        log.error("Unable to get new minting target", exc);
                        throw new InterruptedException("Abormal shutdown");
                    }
                    workers.forEach((worker) -> worker.newTarget(mintingTarget));
                    workDispatched = true;
                }
                //
                // Submit a pending solution
                //
                // We can have just one unconfirmed minting transaction at a time.  So we need to hold
                // additional transactions until a block has been confirmed.  This is usually not a
                // problem but a block occasionally takes 10 minutes or longer to be generated.  If
                // this happens, we will poll the server until a new block has been generated.
                //
                if (!pending.isEmpty()) {
                    boolean submitted = false;
                    try {
                        response = Request.getBlockchainStatus();
                        int height = response.getInt("numberOfBlocks") - 1;
                        if (height > submitHeight) {
                            submitHeight = height;
                            response = Request.getUnconfirmedTransactions(Main.chainId, Main.accountId);
                            if (response.getObjectList("unconfirmedTransactions").isEmpty()) {
                                Solution solution = pending.get(0);
                                response = Request.currencyMint(Main.currencyId, Main.chainId,
                                        solution.getNonce(), Main.mintingUnits, solution.getCounter(),
                                        Main.fee, Main.publicKey);
                                byte[] txBytes = response.getHexString("unsignedTransactionBytes");
                                Transaction tx = new Transaction(txBytes);
                                CurrencyMintingAttachment attachment = new CurrencyMintingAttachment(txBytes);
                                if (!tx.getTransactionType().equals("CurrencyMinting") ||
                                        tx.getSenderId() != Main.accountId ||
                                        tx.getAmount() != 0 ||
                                        tx.getFee() != Main.fee ||
                                        attachment.getCurrencyId() != Main.currencyId) {
                                    log.error("CurrencyMinting transaction returned by the server is incorrect");
                                }
                                byte[] signature = Crypto.sign(txBytes, Main.secretPhrase);
                                System.arraycopy(signature, 0, txBytes, Transaction.SIGNATURE_OFFSET, 64);
                                response = Request.broadcastTransaction(txBytes);
                                solution.setTxId(response.getId("transaction"));
                                if (Main.mainWindow != null)
                                    Main.mainWindow.solutionFound(solution);
                                log.info(String.format("Solution for counter %d submitted", solution.getCounter()));
                                submitted = true;
                                pending.remove(0);
                            }
                        }
                    } catch (NxtException exc) {
                        log.error("Server rejected 'currencyMint' transaction - discarding");
                        submitted = true;
                        pending.remove(0);
                    } catch (IOException exc) {
                        log.error("Unable to submit 'currencyMint' transaction - retrying", exc);
                    }
                    if (!submitted)
                        Thread.sleep(30000);
                }
            }
        } catch (InterruptedException exc) {
            log.info("Minting controller stopping");
        } catch (Throwable exc) {
            log.error("Minting controller terminated by exception", exc);
        }
    }

    /**
     * Stop minting
     */
    public static void shutdown() {
        try {
            //
            // Stop the mint thread
            //
            if (Thread.currentThread() != mintThread) {
                mintThread.interrupt();
                mintThread.join(60000);
            }
            //
            // Stop the worker threads
            //
            workers.forEach((worker) -> worker.shutdown());
        } catch (InterruptedException exc) {
            log.error("Unable to wait for workers to terminate", exc);
        }
    }

    /**
     * Return the list of workers
     *
     * @return                      Worker list
     */
    public static List<MintWorker> getWorkers() {
        return workers;
    }
}
