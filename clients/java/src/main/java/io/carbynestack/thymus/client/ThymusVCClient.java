/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.thymus.client.ThymusVCPClient.ThymusVCPClientBuilder;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A Thymus client to interact with all the Thymus services of a Carbyne Stack
 * Virtual Cloud.
 */
@Slf4j
public class ThymusVCClient {

    private final List<ThymusVCPClient> vcpClients;
    private final Executor executor;

    ThymusVCClient(List<ThymusVCPClient> vcpClients) {
        if (vcpClients.isEmpty()) {
            throw new IllegalArgumentException("At least one VCP client must be provided");
        }
        this.vcpClients = vcpClients;
        this.executor = Executors.newFixedThreadPool(vcpClients.size());
    }

    /**
     * The endpoints this client talks to.
     *
     * @return The list of endpoints
     */
    public List<ThymusEndpoint> getThymusEndpoints() {
        return vcpClients.stream().map(ThymusVCPClient::getEndpoint).collect(Collectors.toList());
    }

    /**
     * Returns the policies available within the Carbyne Stack Virtual Cloud.
     *
     * @return The intersection of the policies registered across all the VCPs in the VC.
     */
    public Future<Either<ThymusError, Set<NamespacedName>>> getPolicies() {
        // Execute all the requests in parallel
        Stream<Future<Tuple2<ThymusEndpoint, Either<ThymusError, List<NamespacedName>>>>> invocations =
                Stream.ofAll(vcpClients).map(c -> Future.of(executor, () -> {
                    log.info("Fetching policies from endpoint {}", c.getEndpoint());
                    return Tuple.of(c.getEndpoint(), c.getPolicies());
                }));

        // Collect the results and translate into a single aggregate result
        return Future.sequence(invocations).map(results -> {
            Set<ThymusError> errors = new HashSet<>();
            Set<NamespacedName> commonPolicies = new HashSet<>();
            Set<NamespacedName> allPolicies = new HashSet<>();
            results.zipWithIndex().forEach(r -> {
                val index = r._2;
                val result = r._1;
                if (result._2.isLeft()) {
                    // Collect all errors indexed by the endpoint
                    errors.add(result._2.getLeft());
                } else {
                    allPolicies.addAll(result._2.get());

                    // Compute the intersection of all policies
                    if (index == 0) {
                        commonPolicies.addAll(result._2.get());
                    } else {
                        commonPolicies.retainAll(result._2.get());
                    }
                }
            });
            if (commonPolicies.size() != allPolicies.size()) {
                allPolicies.removeAll(commonPolicies);
                log.debug("There are policies that are not common across all VCPs: {}", allPolicies);
            }
            if (!errors.isEmpty()) {
                val me = new ThymusError.MultiError().setErrors(errors);
                return Either.left(me);
            }
            return Either.right(commonPolicies);
        });
    }

    /**
     * Returns the policy with the given name.
     *
     * @param name The namespaced name of the policy.
     * @return The policy or an error indicating why the policy could not be fetched. In case VCPs return different
     * policies for the same name, an {@link ThymusError.InconsistentPoliciesError} is returned.
     */
    public Future<Either<ThymusError, Policy>> getPolicy(NamespacedName name) {
        // Execute all the requests in parallel
        Stream<Future<Tuple2<ThymusEndpoint, Either<ThymusError, Policy>>>> invocations =
                Stream.ofAll(vcpClients).map(c -> Future.of(executor, () -> {
                    log.info("Fetching policy '{}' from endpoint {}", name, c.getEndpoint());
                    return Tuple.of(c.getEndpoint(), c.getPolicy(name));
                }));

        // Collect the results and translate into a single aggregate result
        return Future.sequence(invocations).map(results -> {
            Set<ThymusError> errors = new HashSet<>();
            Map<ThymusEndpoint, Policy> uniquePolicies = new HashMap<>();
            results.forEach(r -> {
                val endpoint = r._1;
                val result = r._2;
                result.peekLeft(errors::add).peek(p -> {
                    if (!uniquePolicies.containsValue(p)) {
                        uniquePolicies.put(endpoint, p);
                    }
                });
            });

            // If there are errors, return them
            if (!errors.isEmpty()) {
                val err = new ThymusError.MultiError().setErrors(errors);
                return Either.left(err);
            }

            // Check if the policies are consistent. If there is more than one policy, return an error.
            if (uniquePolicies.size() > 1) {
                val err = new ThymusError.InconsistentPoliciesError().setConflictingPolicies(uniquePolicies);
                return Either.left(err);
            }

            // Return the single unique policy
            return Either.right(uniquePolicies.values().iterator().next());
        });
    }

    /**
     * Provides the bearer token used for authentication.
     */
    public interface BearerTokenProvider {

        /**
         * Returns the bearer token for a Thymus endpoint.
         *
         * @param endpoint The endpoint of the Thymus service for which the token is requested.
         * @return The token
         */
        String getBearerToken(ThymusEndpoint endpoint);
    }

    /**
     * Builder class to create a new {@link ThymusVCClient}.
     */
    public static class Builder {

        private List<ThymusEndpoint> endpoints;
        private final List<File> trustedCertificates;
        private boolean sslValidationEnabled = true;
        private Option<BearerTokenProvider> bearerTokenProvider;
        private ThymusVCPClientBuilder thymusClientBuilder = ThymusVCPClient.Builder();

        public Builder() {
            this.endpoints = new ArrayList<>();
            this.trustedCertificates = new ArrayList<>();
            bearerTokenProvider = Option.none();
        }

        /**
         * Adds a Thymus service endpoint to the list of endpoints, the client should communicate
         * with.
         *
         * @param endpoint Endpoint of a backend Thymus Service
         */
        public Builder withEndpoint(ThymusEndpoint endpoint) {
            this.endpoints.add(endpoint);
            return this;
        }

        /**
         * The client will be initialized to communicate with the given endpoints. All endpoints that
         * have been added before will be replaced. To add additional endpoints use {@link
         * #withEndpoint(ThymusEndpoint)}.
         *
         * @param endpoints A List of endpoints which will be used to communicate with.
         */
        public Builder withEndpoints(@NonNull List<ThymusEndpoint> endpoints) {
            this.endpoints = new ArrayList<>(endpoints);
            return this;
        }

        /**
         * Controls whether SSL certificate validation is performed.
         *
         * <p>
         *
         * <p><b>WARNING</b><br>
         * Please be aware, that disabling validation leads to insecure web connections and is meant to
         * be used in a local test setup only. Using this option in a productive environment is
         * explicitly <u>not recommended</u>.
         *
         * @param enabled <tt>true</tt>, in case SSL certificate validation should happen,
         *                <tt>false</tt> otherwise
         */
        public Builder withSslCertificateValidation(boolean enabled) {
            this.sslValidationEnabled = enabled;
            return this;
        }

        /**
         * Adds a certificate (.pem) to the trust store.<br>
         * This allows tls secured communication with services that do not have a certificate issued by
         * an official CA (certificate authority).
         *
         * @param trustedCertificate Public certificate.
         */
        public Builder withTrustedCertificate(File trustedCertificate) {
            this.trustedCertificates.add(trustedCertificate);
            return this;
        }

        /**
         * Sets a provider for getting a backend specific bearer token that is injected as an
         * authorization header to REST HTTP calls emitted by the client.
         *
         * @param bearerTokenProvider Provider for backend specific bearer token
         */
        public Builder withBearerTokenProvider(BearerTokenProvider bearerTokenProvider) {
            this.bearerTokenProvider = Option.of(bearerTokenProvider);
            return this;
        }

        protected Builder withThymusVCPClientBuilder(ThymusVCPClientBuilder thymusClientBuilder) {
            this.thymusClientBuilder = thymusClientBuilder;
            return this;
        }

        /**
         * Builds and returns a new {@link ThymusVCClient} according to the given configuration.
         *
         * @throws CsHttpClientException If the client could not be instantiated.
         */
        public ThymusVCClient build() throws CsHttpClientException {
            if (this.endpoints == null || this.endpoints.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one Thymus service endpoint has to be provided.");
            }
            List<ThymusVCPClient> clients = Try.sequence(
                            this.endpoints.stream().map(endpoint -> Try.of(() -> {
                                        ThymusVCPClientBuilder b =
                                                thymusClientBuilder
                                                        .withEndpoint(endpoint)
                                                        .withoutSslValidation(!this.sslValidationEnabled)
                                                        .withTrustedCertificates(this.trustedCertificates);
                                        this.bearerTokenProvider.forEach(p ->
                                                b.withBearerToken(
                                                        Option.of(p.getBearerToken(endpoint))));
                                        return b.build();
                                    }))
                                    .collect(Collectors.toList()))
                    .getOrElseThrow(CsHttpClientException::new)
                    .toJavaList();
            return new ThymusVCClient(clients);
        }
    }

}
