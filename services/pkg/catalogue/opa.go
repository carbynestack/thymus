// Copyright (c) 2024 - for information on the respective copyright owner
// see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
//
// SPDX-License-Identifier: Apache-2.0

package catalogue

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
)

const BasePath = "v1/policies"

// Result is the response from the OPA service
type Result struct {
	Policies []Policy `json:"result"`
}

// Policy represents a policy in the OPA service
type Policy struct {
	ID   string `json:"id"`
	Code string `json:"raw"`
	Ast  string `json:"-"` // Will be ignored
}

// fetchPoliciesFromOPA fetches all policies from the OPA service. Note that
// there is unfortunately no pagination support.
func fetchPoliciesFromOPA(opaSvcUrl string) (policies map[string]Policy, error error) {
	resp, err := http.Get(fmt.Sprintf("%s/%s", opaSvcUrl, BasePath))
	if err != nil {
		return nil, err
	}
	defer closeBody(resp.Body, &error)

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	result := Result{}
	err = json.Unmarshal(body, &result)
	if err != nil {
		return nil, err
	}

	policies = make(map[string]Policy)
	for _, policy := range result.Policies {
		policies[policy.ID] = policy
	}
	return policies, nil
}

// fetchPolicyFromOPA fetches a single policy from the OPA service by its ID.
// Note that there is no way to fetch a single policy by ID, so we fetch all.
func fetchPolicyFromOPA(opaSvcUrl string, id string) (*Policy, error) {
	policies, err := fetchPoliciesFromOPA(opaSvcUrl)
	if err != nil {
		return nil, err
	}
	p, ok := policies[id]
	if !ok {
		return nil, nil
	}
	return &p, nil
}

// closeBody closes the response body and updates the error if needed
func closeBody(body io.ReadCloser, err *error) {
	if cerr := body.Close(); cerr != nil {
		if *err != nil {
			*err = cerr
		} else {
			*err = fmt.Errorf("error closing response body: %w", cerr)
		}
	}
}
