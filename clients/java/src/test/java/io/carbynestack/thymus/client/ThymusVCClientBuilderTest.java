/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.thymus.client.ThymusVCPClient.ThymusVCPClientBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ThymusVCClientBuilderTest {

    private static final List<ThymusEndpoint> ENDPOINTS =
            Stream.of("https://testUri:80", "https://testUri:180")
                    .map(url -> ThymusEndpoint.Builder()
                            .withServiceUri(URI.create(url))
                            .build())
                    .collect(Collectors.toList());

    @Mock
    private ThymusVCPClientBuilder thymusClientBuilder;

    @Test
    public void givenEndpointList_whenBuilding_createsClientWithCorrectEndpoints()
            throws CsHttpClientException {
        ThymusVCClient client =
                new ThymusVCClient.Builder().withEndpoints(ENDPOINTS).build();
        assertThat(
                client.getThymusEndpoints(),
                CoreMatchers.hasItems(ENDPOINTS.toArray(new ThymusEndpoint[0])));
    }

    @Test
    public void givenIndividualEndpoints_whenBuilding_createsClientWithCorrectEndpoints()
            throws CsHttpClientException {
        ThymusVCClient.Builder builder = new ThymusVCClient.Builder();
        ENDPOINTS.forEach(builder::withEndpoint);
        ThymusVCClient client = builder.build();
        assertThat(
                client.getThymusEndpoints(),
                CoreMatchers.hasItems(ENDPOINTS.toArray(new ThymusEndpoint[0])));
    }

    @Test
    public void
    givenSslCertificateValidationDisabledOnBuilder_whenBuilding_createsUnderlyingClientsWithSslCertificateValidationDisabled()
            throws CsHttpClientException {
        when(thymusClientBuilder.withEndpoint(any())).thenReturn(thymusClientBuilder);
        when(thymusClientBuilder.withoutSslValidation(anyBoolean()))
                .thenReturn(thymusClientBuilder);
        when(thymusClientBuilder.withTrustedCertificates(any())).thenReturn(thymusClientBuilder);
        new ThymusVCClient.Builder()
                .withThymusVCPClientBuilder(thymusClientBuilder)
                .withEndpoints(ENDPOINTS)
                .withSslCertificateValidation(false)
                .build();
        verify(thymusClientBuilder, times(2)).withoutSslValidation(true);
    }

    @Test
    public void
    givenTrustedCertificateProvidedToBuilder_whenBuilding_createsUnderlyingClientsWithCertificatesAdded()
            throws IOException {
        File cert = File.createTempFile("test", ".pem");
        when(thymusClientBuilder.withEndpoint(any())).thenReturn(thymusClientBuilder);
        when(thymusClientBuilder.withoutSslValidation(anyBoolean()))
                .thenReturn(thymusClientBuilder);
        when(thymusClientBuilder.withTrustedCertificates(any())).thenReturn(thymusClientBuilder);
        new ThymusVCClient.Builder()
                .withThymusVCPClientBuilder(thymusClientBuilder)
                .withEndpoints(ENDPOINTS)
                .withTrustedCertificate(cert)
                .build();
        verify(thymusClientBuilder, times(2))
                .withTrustedCertificates(Collections.singletonList(cert));
    }

}
