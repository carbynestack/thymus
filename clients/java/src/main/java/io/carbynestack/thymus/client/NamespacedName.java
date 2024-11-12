/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Thymus policy name composed of a namespace and a name,
 * e.g., "default-policies:donor-read.rego", where "default-policies" is the
 * namespace and "donor-read.rego" is the name.
 */
@Value
public class NamespacedName {

    private final static Pattern NAMESPACE_PATTERN = Pattern.compile("^([^:]+):([^:]+)$");

    /**
     * Parses a namespaced name from a string.
     *
     * @param str The namespaced name in the format "namespace:name".
     * @return The parsed namespaced name.
     * @throws IllegalArgumentException If the string doesn't match the
     *                                  {@link NamespacedName#NAMESPACE_PATTERN}.
     */
    public static NamespacedName fromString(String str) {
        Matcher matcher = NAMESPACE_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid namespaced name: " + str);
        }
        return new NamespacedName(matcher.group(1), matcher.group(2));
    }

    /**
     * The namespace of the policy.
     */
    String namespace;

    /**
     * The name of the policy.
     */
    String name;

    /**
     * Returns the namespaced name as a string in the format "namespace:name".
     *
     * @return The namespaced name as a string.
     */
    public String toString() {
        return namespace + ":" + name;
    }

}
