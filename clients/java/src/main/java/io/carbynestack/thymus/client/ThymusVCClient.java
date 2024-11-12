/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

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
            results.zipWithIndex().forEach(r -> {
                val index = r._2;
                val result = r._1;
                if (result._2.isLeft()) {
                    // Collect all errors indexed by the endpoint
                    errors.add(result._2.getLeft());
                } else {
                    // Compute the intersection of all policies
                    if (index == 0) {
                        commonPolicies.addAll(result._2.get());
                    } else {
                        commonPolicies.retainAll(result._2.get());
                    }
                }
            });
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

}
