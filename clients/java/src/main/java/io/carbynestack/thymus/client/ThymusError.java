/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.CsHttpClientException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

/**
 * Error returned by {@link ThymusVCPClient} and {@link ThymusVCClient} in case an error
 * occurs.
 */
public interface ThymusError {

    /**
     * Indicates that an IO error occurred while interacting with a VCP Thymus
     * service.
     */
    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    class ThymusIOError implements ThymusError {

        CsHttpClientException exception;

    }


    /**
     * Indicates that an error occurred while interacting with a VCP Thymus
     * service.
     */
    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    class ThymusServiceError implements ThymusError {

        /**
         * The endpoint that caused the error.
         */
        ThymusEndpoint endpoint;

        /**
         * An HTTP status code.
         */
        Integer responseCode;

        /**
         * A human-readable error message.
         */
        String message;
    }

    /**
     * Indicates that the policies returned by VCP services are inconsistent.
     */
    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    class InconsistentPoliciesError implements ThymusError {

        /**
         * The conflicting policies.
         */
        Map<ThymusEndpoint, Policy> conflictingPolicies;
    }

    /**
     * Indicates that multiple errors occurred.
     **/
    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    class MultiError implements ThymusError {

        /**
         * The errors that occurred.
         */
        Set<ThymusError> errors;
    }

}
