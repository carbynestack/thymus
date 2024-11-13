/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Policy response as returned by the Thymus Policy Catalogue service.
 */
@Value
@NoArgsConstructor(force = true)
public class PolicyResponse {

    private static final Pattern POLICY_ID_PATTERN = Pattern.compile("([^/]+):([^/]+)");

    String id;

    String raw;

    public PolicyResponse(Policy policy) {
        this.id = policy.getName().getNamespace() + ":" + policy.getName().getName();
        this.raw = policy.getSource();
    }


    public Policy asPolicy() {
        Matcher matcher = POLICY_ID_PATTERN.matcher(id);
        if (matcher.matches()) {
            String namespace = matcher.group(1);
            String name = matcher.group(2);
            NamespacedName namespacedName = new NamespacedName(namespace, name);
            return new Policy(namespacedName, raw);
        } else {
            throw new IllegalArgumentException("Invalid policy ID format: " + id);
        }
    }

}
