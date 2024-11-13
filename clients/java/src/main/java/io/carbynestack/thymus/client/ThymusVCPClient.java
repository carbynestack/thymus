/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.BearerTokenUtils;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.httpclient.CsResponseEntity;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Thymus client to interact with the Thymus service of a Carbyne Stack
 * Virtual Cloud Provider.
 */
@Slf4j
public class ThymusVCPClient {

    static final Header ACCEPT_JSON_HEADER =
            new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

    @Getter(value = AccessLevel.PACKAGE)
    private final ThymusEndpoint endpoint;

    private final CsHttpClient<String> csHttpClient;
    private final Option<String> bearerToken;

    @lombok.Builder(builderMethodName = "Builder")
    private ThymusVCPClient(
            @NonNull ThymusEndpoint withEndpoint,
            List<File> withTrustedCertificates,
            boolean withoutSslValidation,
            Option<String> withBearerToken)
            throws CsHttpClientException {
        this(
                withEndpoint,
                CsHttpClient.<String>builder()
                        .withFailureType(String.class)
                        .withTrustedCertificates(withTrustedCertificates)
                        .withoutSslValidation(withoutSslValidation)
                        .build(),
                withBearerToken == null ? Option.none() : withBearerToken);
    }

    ThymusVCPClient(
            ThymusEndpoint endpoint, CsHttpClient<String> csHttpClient, Option<String> bearerToken)
            throws CsHttpClientException {
        if (endpoint == null) {
            throw new CsHttpClientException("Endpoint must not be null.");
        }
        this.endpoint = endpoint;
        this.csHttpClient = csHttpClient;
        this.bearerToken = bearerToken;
    }

    /**
     * Returns the names of all available policies.
     *
     * @return Either the names of the available policies or an error if the
     * request failed.
     */
    public Either<ThymusError, List<NamespacedName>> getPolicies() {
        try {
            URI policiesUri = endpoint.getPoliciesUri();
            List<Header> headers = bearerToken
                    .map(BearerTokenUtils::createBearerToken)
                    .collect(Collectors.toList());
            headers.add(ACCEPT_JSON_HEADER);
            CsResponseEntity<String, String[]> responseEntity =
                    csHttpClient.getForEntity(policiesUri, headers, String[].class);
            return responseEntity
                    .getContent()
                    .map(r -> Arrays.stream(r)
                            .map(NamespacedName::fromString)
                            .collect(Collectors.toList()))
                    .mapLeft(
                            l -> new ThymusError.ThymusServiceError()
                                    .setEndpoint(endpoint)
                                    .setMessage(l)
                                    .setResponseCode(responseEntity.getHttpStatus()));
        } catch (CsHttpClientException e) {
            return Either.left(new ThymusError.ThymusIOError().setException(e));
        }
    }

    /**
     * Returns a policy by its namespaced name.
     *
     * @param name The namespaced name of the policy.
     * @return Either the requested policy or an error if the request failed.
     */
    public Either<ThymusError, Policy> getPolicy(NamespacedName name) {
        try {
            URI policyUri = endpoint.getPolicyUri(name);
            List<Header> headers = bearerToken
                    .map(BearerTokenUtils::createBearerToken)
                    .toJavaList();
            headers.add(ACCEPT_JSON_HEADER);
            CsResponseEntity<String, PolicyResponse> responseEntity =
                    csHttpClient.getForEntity(policyUri, headers, PolicyResponse.class);
            return responseEntity
                    .getContent()
                    .map(PolicyResponse::asPolicy)
                    .mapLeft(l -> new ThymusError.ThymusServiceError()
                            .setEndpoint(endpoint)
                            .setMessage(l)
                            .setResponseCode(responseEntity.getHttpStatus()));
        } catch (CsHttpClientException e) {
            return Either.left(new ThymusError.ThymusIOError().setException(e));
        }
    }

}
