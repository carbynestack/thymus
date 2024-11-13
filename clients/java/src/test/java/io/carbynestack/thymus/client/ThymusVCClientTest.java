/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.vavr.control.Either;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThymusVCClientTest {

    private static final Policy POLICY_A = new Policy(new NamespacedName("thymus-test", "a"), "a");
    private static final Policy POLICY_B = new Policy(new NamespacedName("thymus-test", "b"), "b");
    private static final Policy POLICY_C = new Policy(new NamespacedName("thymus-test", "c"), "c");
    private static final Policy POLICY_A_MODIFIED = new Policy(new NamespacedName("thymus-test", "a"), "b");
    private static final List<NamespacedName> POLICY_NAMES_A = Arrays.asList(POLICY_A.getName(), POLICY_B.getName());
    private static final List<NamespacedName> POLICY_NAMES_B = Arrays.asList(POLICY_B.getName(), POLICY_C.getName());

    @Mock
    private ThymusVCPClient thymusVCPClientA;

    @Mock
    private ThymusVCPClient thymusVCPClientB;

    private ThymusVCClient thymusVCClient;

    @Before
    public void setUp() {
        val clients = Arrays.asList(thymusVCPClientA, thymusVCPClientB);
        thymusVCClient = new ThymusVCClient(clients);
        for (val client : clients) {
            when(client.getEndpoint()).thenReturn(new ThymusEndpoint(URI.create("http://host-" + client.hashCode())));
        }
    }

    @Test
    public void givenNoClients_whenCreateClient_thenThrowException() {
        val e = assertThrows(IllegalArgumentException.class, () -> new ThymusVCClient(Collections.emptyList()));
        assertThat(e.getMessage(), containsString("At least one VCP client must be provided"));
    }

    @Test
    public void whenGetCEndpoints_thenReturnEndpoint() {
        val result = thymusVCClient.getThymusEndpoints();
        assertThat(result, hasSize(2));
        assertThat(result, containsInAnyOrder(thymusVCPClientA.getEndpoint(), thymusVCPClientB.getEndpoint()));
    }

    @Test
    public void givenSuccessfulWithConsistentPolicies_whenGetPolicies_thenReturnPolicies() {
        when(thymusVCPClientA.getPolicies()).thenReturn(Either.right(POLICY_NAMES_A));
        when(thymusVCPClientB.getPolicies()).thenReturn(Either.right(POLICY_NAMES_A));
        val f = thymusVCClient.getPolicies();
        val result = f.await().get();
        assertTrue(result.isRight());
        assertThat(result.get(), hasSize(POLICY_NAMES_A.size()));
        assertThat(result.get(), containsInAnyOrder(POLICY_NAMES_A.toArray()));
    }

    @Test
    public void givenSuccessfulWithDifferingPolicies_whenGetPolicies_thenReturnIntersection() {
        when(thymusVCPClientA.getPolicies()).thenReturn(Either.right(POLICY_NAMES_A));
        when(thymusVCPClientB.getPolicies()).thenReturn(Either.right(POLICY_NAMES_B));
        val f = thymusVCClient.getPolicies();
        val result = f.await().get();
        assertTrue(result.isRight());
        assertThat(result.get(), hasSize(1));
        assertThat(result.get(), containsInAnyOrder(POLICY_B.getName()));
    }

    @Test
    public void givenUnsuccessful_whenGetPolicies_thenReturnError() {
        val err = new ThymusError.ThymusServiceError()
                .setResponseCode(500)
                .setMessage("Internal Server Error");
        when(thymusVCPClientA.getPolicies()).thenReturn(Either.left(err));
        when(thymusVCPClientB.getPolicies()).thenReturn(Either.right(POLICY_NAMES_A));
        val f = thymusVCClient.getPolicies();
        val result = f.await().get();
        assertTrue(result.isLeft());
        assertThat(result.getLeft(), instanceOf(ThymusError.MultiError.class));
        val multiError = (ThymusError.MultiError) result.getLeft();
        assertThat(multiError.getErrors(), hasSize(1));
        assertThat(multiError.getErrors().iterator().next(), equalTo(err));
    }

    @Test
    public void givenSuccessfulWithConsistentPolicy_whenGetPolicy_thenReturnPolicy() {
        when(thymusVCPClientA.getPolicy(POLICY_A.getName())).thenReturn(Either.right(POLICY_A));
        when(thymusVCPClientB.getPolicy(POLICY_A.getName())).thenReturn(Either.right(POLICY_A));
        val f = thymusVCClient.getPolicy(POLICY_A.getName());
        val result = f.await().get();
        assertTrue(result.isRight());
        assertThat(result.get(), equalTo(POLICY_A));
    }

    @Test
    public void givenSuccessfulWithDifferingPolicies_whenGetPolicy_thenReturnError() {
        when(thymusVCPClientA.getPolicy(POLICY_A.getName())).thenReturn(Either.right(POLICY_A));
        when(thymusVCPClientB.getPolicy(POLICY_A.getName())).thenReturn(Either.right(POLICY_A_MODIFIED));
        val f = thymusVCClient.getPolicy(POLICY_A.getName());
        val result = f.await().get();
        assertTrue(result.isLeft());
        assertThat(result.getLeft(), instanceOf(ThymusError.InconsistentPoliciesError.class));
    }

    @Test
    public void givenUnsuccessful_whenGetPolicy_thenReturnError() {
        val err = new ThymusError.ThymusServiceError()
                .setResponseCode(500)
                .setMessage("Internal Server Error");
        when(thymusVCPClientA.getPolicy(POLICY_A.getName())).thenReturn(Either.left(err));
        when(thymusVCPClientB.getPolicy(POLICY_A.getName())).thenReturn(Either.right(POLICY_A));
        val f = thymusVCClient.getPolicy(POLICY_A.getName());
        val result = f.await().get();
        assertTrue(result.isLeft());
        assertThat(result.getLeft(), instanceOf(ThymusError.MultiError.class));
        val multiError = (ThymusError.MultiError) result.getLeft();
        assertThat(multiError.getErrors(), hasSize(1));
        assertThat(multiError.getErrors().iterator().next(), equalTo(err));
    }

}
