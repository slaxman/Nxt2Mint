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

import java.io.IOException;

/**
 * A NxtException is thrown when the Nxt server rejects an API request
 */
public class NxtException extends IOException {

    /** Nxt error code */
    private final int errorCode;

    /** Nxt error description */
    private final String errorDescription;

    /**
     * Creates a new exception with a detail message
     *
     * @param       msg                 Detail message
     * @param       errorCode           Nxt error code
     * @param       errorDescription    Nxt error description
     */
    public NxtException(String msg, int errorCode, String errorDescription) {
        super(msg);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * Return the Nxt error code
     *
     * @return                      Nxt error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Return the Nxt error description
     *
     * @return                      Nxt error description
     */
    public String getErrorDescription() {
        return errorDescription;
    }
}
