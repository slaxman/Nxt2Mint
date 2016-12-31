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

import org.ScripterRon.JSON.JSONObject;
import org.ScripterRon.JSON.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Send an API request to the Nxt server
 */
public class Request {

    /** UTF-8 character set */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** Default connect timeout (milliseconds) */
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /** Default read timeout (milliseconds) */
    private static final int DEFAULT_READ_TIMEOUT = 30000;

    /** SSL initialized */
    private static volatile boolean sslInitialized = false;

    /**
     * Broadcast a signed transaction
     *
     * @param       transactionBytes        Transaction bytes
     * @return                              Broadcast response
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response broadcastTransaction(byte[] transactionBytes) throws IOException {
        return issueRequest("broadcastTransaction",
                "transactionBytes=" + Utils.toHexString(transactionBytes),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Create a mint transaction and return the unsigned transaction
     *
     * @param       currencyId              Currency identifier
     * @param       chainId                 Chain identifier
     * @param       nonce                   Minting nonce
     * @param       units                   Units minted
     * @param       counter                 Minting counter
     * @param       fee                     Transaction fee
     * @param       publicKey               Sender public key
     * @return                              Transaction
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response currencyMint(long currencyId, int chainId, long nonce, long units,
                                            long counter, long fee, byte[] publicKey)
                                            throws IOException {
        return issueRequest("currencyMint",
                String.format("currency=%s&chain=%s&nonce=%d&units=%d&counter=%d&"
                                + "feeNQT=%s&publicKey=%s&deadline=30&broadcast=false",
                        Utils.idToString(currencyId), Main.chains.get(chainId),
                        nonce, units, counter, Long.toUnsignedString(fee),
                        Utils.toHexString(publicKey)),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the account balance
     *
     * @param       accountId               Account identifier
     * @param       chainId                 Chain identifier
     * @return                              Account balance
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getBalance(long accountId, int chainId) throws IOException {
        return issueRequest("getBalance",
                String.format("account=%s&chain=%s",
                        Utils.idToString(accountId), Main.chains.get(chainId)),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the blockchain status
     *
     * @return                              Blockchain status
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getBlockchainStatus() throws IOException {
        return issueRequest("getBlockchainStatus", null, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the server constants
     *
     * @return                              Server constants
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getConstants() throws IOException {
        return issueRequest("getConstants", null, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get a currency
     *
     * @param       code                    Currency code
     * @param       chainId                 Chain identifier
     * @return                              Currency response
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getCurrency(String code, int chainId) throws IOException {
        return issueRequest("getCurrency",
                String.format("code=%s&chain=%s", code, Main.chains.get(chainId)),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the minting target
     *
     * @param       currencyId              Currency identifier
     * @param       accountId               Account identifier
     * @param       units                   Units to be minted
     * @return                              Target response
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getMintingTarget(long currencyId, long accountId, long units) throws IOException {
        return issueRequest("getMintingTarget",
                String.format("currency=%s&account=%s&units=%d",
                        Utils.idToString(currencyId), Utils.idToString(accountId), units),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the unconfirmed transactions for an account
     *
     * @param       chainId                 Chain identifier
     * @param       accountId               Account identifier
     * @return                              Target response
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getUnconfirmedTransactions(int chainId, long accountId) throws IOException {
        return issueRequest("getUnconfirmedTransactions",
                String.format("chain=%s&account=%s",
                        Main.chains.get(chainId), Utils.idToString(accountId)),
                DEFAULT_READ_TIMEOUT);
    }

    /**
     * Issue the Nxt API request and return the parsed JSON response
     *
     * @param       requestType             Request type
     * @param       requestParams           Request parameters
     * @param       readTimeout             Read timeout (milliseconds)
     * @return                              Parsed JSON response
     * @throws      IOException             Unable to issue Nxt API request
     */
    @SuppressWarnings("unchecked")
    private static Response issueRequest(String requestType, String requestParams, int readTimeout)
                                            throws IOException {
        Response response = null;
        if (Main.useSSL && !sslInitialized)
            sslInit();
        try {
            URL url = new URL(String.format("%s://%s:%d/nxt",
                    (Main.nxtHost.equals("localhost") ? "http" :
                            (Main.useSSL ? "https" : "http")), Main.nxtHost, Main.apiPort));
            String request;
            if (requestParams != null)
                request = String.format("requestType=%s&%s", requestType, requestParams);
            else
                request = String.format("requestType=%s", requestType);
            byte[] requestBytes = request.getBytes(UTF8);
            Main.log.debug(String.format("Issue HTTP request to %s:%d: %s",
                    Main.nxtHost, Main.apiPort, request));
            //
            // Issue the request
            //
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Cache-Control", "no-cache, no-store");
            conn.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            conn.setReadTimeout(readTimeout);
            conn.connect();
            try (OutputStream out = conn.getOutputStream()) {
                out.write(requestBytes);
                out.flush();
                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    String errorText = String.format("Response code %d for %s request\n  %s",
                                                     code, requestType, conn.getResponseMessage());
                    Main.log.error(errorText);
                    throw new IOException(errorText);
                }
            }
            //
            // Parse the response
            //
            String contentEncoding = conn.getHeaderField("Content-Encoding");
            try (InputStream in = conn.getInputStream()) {
                InputStreamReader reader;
                if ("gzip".equals(contentEncoding))
                    reader = new InputStreamReader(new GZIPInputStream(in), UTF8);
                else
                    reader = new InputStreamReader(in, UTF8);
                Object respObject = JSONParser.parse(reader);
                if (!(respObject instanceof JSONObject))
                    throw new IOException("Server response is not a JSON object");
                response = new Response((Map<String, Object>)respObject);
                Long errorCode = (Long)response.get("errorCode");
                if (errorCode != null) {
                    String errorDesc = (String)response.get("errorDescription");
                    String errorText = String.format("Error %d returned for %s request: %s",
                                                     errorCode, requestType, errorDesc);
                    Main.log.error(errorText);
                    throw new NxtException(errorText, errorCode.intValue(), errorDesc);
                }
            }
            if (Main.log.isDebugEnabled())
                Main.log.debug(String.format("Request complete: Content-Encoding %s\n%s",
                                        contentEncoding, Utils.formatJSON(response.getObjectMap())));
        } catch (ParseException exc) {
            String errorText = String.format("JSON parse exception for %s request: Position %d: %s",
                                             requestType, exc.getErrorOffset(), exc.getMessage());
            Main.log.error(errorText);
            throw new IOException(errorText);
        } catch (IOException exc) {
            String errorText = String.format("I/O error on %s request", requestType);
            Main.log.error(errorText, exc);
            throw new IOException(errorText, exc);
        }
        return response;
    }

    /**
     * SSL initialization
     */
    private static void sslInit() {
        try {
            //
            // Create the SSL context
            //
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] tm = (Main.acceptAnyCertificate ? new TrustManager[] {new AllCertificates()} : null);
            context.init(null, tm, new SecureRandom());
            //
            // Set default values for HTTPS connections
            //
            HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            sslInitialized = true;
        } catch (NoSuchAlgorithmException exc) {
            Main.log.error("TLS algorithm is not available", exc);
            throw new IllegalStateException("TLS algorithm is not available");
        } catch (KeyManagementException exc) {
            Main.log.error("Unable to initialize SSL context", exc);
            throw new IllegalStateException("Unable to initialize SSL context", exc);
        }
    }

    /**
     * Certificate host name verifier
     */
    private static class NameVerifier implements HostnameVerifier {

        /**
         * Check if a certificate host name mismatch is allowed
         *
         * @param       hostname            URL host name
         * @param       session             SSL session
         * @return                          TRUE if the mismatch is allowed
         */
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return Main.allowNameMismatch;
        }
    }

    /**
     * Certificate trust manager to accept all certificates
     */
    private static class AllCertificates implements X509TrustManager {

        /**
         * Return a list of accepted certificate issuers
         *
         * Since we accept all certificates, we will return an empty certificate list.
         *
         * @return                          Empty certificate list
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        /**
         * Build the certificate path to a trusted root certificate
         *
         * Since we accept all certificates, we will simply return
         *
         * @param   certs                   Certificate chain
         * @param   authType                Authentication type
         */
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType)
                                            throws CertificateException {
        }

        /**
         * Build the certificate path to a trusted root certificate
         *
         * Since we accept all certificates, we will simply return
         *
         * @param   certs                   Certificate chain
         * @param   authType                Authentication type
         */
        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                                            throws CertificateException {
        }
    }
}
