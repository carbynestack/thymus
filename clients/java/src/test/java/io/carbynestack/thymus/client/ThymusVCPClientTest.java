/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.httpclient.CsResponseEntity;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThymusVCPClientTest {

    private static final URI TEST_URI = Try.of(() -> new URI("http://localhost")).get();
    private static final ThymusEndpoint ENDPOINT = new ThymusEndpoint(TEST_URI);
    private static final Policy TEST_POLICY = new Policy(
            new NamespacedName("ns", "name"),
            "{ \"id\": \"stackable/bundles/a/b\", \"raw\": \"package c\" }");

    @Mock
    private CsHttpClient<String> csHttpClient;

    private ThymusVCPClient client;

    @Before
    public void setUp() throws CsHttpClientException {
        client = new ThymusVCPClient(ENDPOINT, csHttpClient, Option.none());
    }

    @Test
    public void givenServiceUrlIsNull_whenCreateClient_thenThrowException() {
        CsHttpClientException cce =
                assertThrows(CsHttpClientException.class, () -> ThymusVCPClient.Builder().build());
        assertThat(cce.getMessage(), containsString("Endpoint must not be null."));
    }

    @Test
    public void givenSuccessful_whenGetPolicies_thenReturnPolicies() throws CsHttpClientException {
        List<NamespacedName> result = Collections.singletonList(TEST_POLICY.getName());
        when(csHttpClient.getForEntity(
                ENDPOINT.getPoliciesUri(),
                Collections.singletonList(ThymusVCPClient.ACCEPT_JSON_HEADER),
                String[].class))
                .thenReturn(CsResponseEntity.success(200,
                        result.stream().map(NamespacedName::toString).toArray(String[]::new)));
        Either<ThymusError, List<NamespacedName>> response = client.getPolicies();
        assertTrue(response.isRight());
        assertThat(response.get(), equalTo(result));
    }

    @Test
    public void givenUnsuccessful_whenGetPolicies_thenReturnError() throws CsHttpClientException {
        int httpFailureCode = 500;
        String errorMessage = "failed to fetch policies";
        when(csHttpClient.getForEntity(
                ENDPOINT.getPoliciesUri(),
                Collections.singletonList(ThymusVCPClient.ACCEPT_JSON_HEADER),
                String[].class))
                .thenReturn(CsResponseEntity.failed(httpFailureCode, errorMessage));
        Either<ThymusError, List<NamespacedName>> response = client.getPolicies();
        assertTrue(response.isLeft());
        assertTrue(response.getLeft() instanceof ThymusError.ThymusServiceError);
        ThymusError.ThymusServiceError error = (ThymusError.ThymusServiceError) response.getLeft();
        assertThat(error.responseCode, equalTo(httpFailureCode));
        assertThat(error.message, equalTo(errorMessage));
    }

    @Test
    public void givenSuccessful_whenGetPolicy_thenReturnPolicy() throws CsHttpClientException {
        when(csHttpClient.getForEntity(
                ENDPOINT.getPolicyUri(TEST_POLICY.getName()),
                Collections.singletonList(ThymusVCPClient.ACCEPT_JSON_HEADER),
                PolicyResponse.class))
                .thenReturn(CsResponseEntity.success(200, new PolicyResponse(TEST_POLICY)));
        Either<ThymusError, Policy> response = client.getPolicy(TEST_POLICY.getName());
        assertTrue(response.isRight());
        assertThat(response.get(), equalTo(TEST_POLICY));
    }

    @Test
    public void givenUnsuccessful_whenGetPolicy_thenReturnError() throws CsHttpClientException {
        int httpFailureCode = 500;
        String errorMessage = "failed to fetch policy";
        when(csHttpClient.getForEntity(
                ENDPOINT.getPolicyUri(TEST_POLICY.getName()),
                Collections.singletonList(ThymusVCPClient.ACCEPT_JSON_HEADER),
                PolicyResponse.class))
                .thenReturn(CsResponseEntity.failed(httpFailureCode, errorMessage));
        Either<ThymusError, Policy> response = client.getPolicy(TEST_POLICY.getName());
        assertTrue(response.isLeft());
        assertTrue(response.getLeft() instanceof ThymusError.ThymusServiceError);
        ThymusError.ThymusServiceError error = (ThymusError.ThymusServiceError) response.getLeft();
        assertThat(error.responseCode, equalTo(httpFailureCode));
        assertThat(error.message, equalTo(errorMessage));
    }

}
