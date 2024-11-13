/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.thymus.client;

import lombok.Value;

/**
 * A Thymus authorization policy.
 */
@Value
public class Policy {

    /**
     * The namespaced name of the policy.
     */
    NamespacedName name;

    /**
     * The source code of the policy.
     */
    String source;

}
