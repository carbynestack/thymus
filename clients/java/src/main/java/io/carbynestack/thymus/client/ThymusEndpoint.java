/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.CsHttpClientException;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;

/**
 * A Thymus service endpoint consisting of the Thymus service URI.
 */
@Value
public class ThymusEndpoint {

    URI serviceUri;

    /**
     * @param withServiceUri The URL of the Ephemeral service.
     */
    @Builder(builderMethodName = "Builder")
    public ThymusEndpoint(@NonNull URI withServiceUri) {
        this.serviceUri = withServiceUri;
    }

    /**
     * Returns the URI of the policies endpoint.
     */
    public URI getPoliciesUri() throws CsHttpClientException {
        URIBuilder uriBuilder = new URIBuilder(serviceUri);
        uriBuilder.setPath(uriBuilder.getPath());
        return Try.of(uriBuilder::build).getOrElseThrow(CsHttpClientException::new);
    }

    /**
     * Returns the URI of the policy endpoint.
     *
     * @param name The namespaced name of the policy.
     * @return The URI of the policy endpoint.
     * @throws CsHttpClientException If the URI cannot be built.
     */
    public URI getPolicyUri(NamespacedName name) throws CsHttpClientException {
        URIBuilder uriBuilder = new URIBuilder(serviceUri);
        String path = uriBuilder.getPath();
        uriBuilder.setPath((path != null ? uriBuilder.getPath() + "/" : "") + name.toString());
        return Try.of(uriBuilder::build).getOrElseThrow(CsHttpClientException::new);
    }

}
