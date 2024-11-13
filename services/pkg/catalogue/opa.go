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

// Policy represents a policy in the OPA service
type Policy struct {
	ID   string `json:"id"`
	Code string `json:"raw"`
	Ast  string `json:"-"` // Will be ignored
}

// ListPoliciesResult is the response from the OPA service when invoking the
// list policies endpoint
type ListPoliciesResult struct {
	Policies []Policy `json:"result"`
}

// GetPolicyResult is the response from the OPA service when invoking the
// get policy by ID endpoint
type GetPolicyResult struct {
	Policy Policy `json:"result"`
}

// PolicyNotFound is an error type that is returned when a policy with the given
// ID is not found
type PolicyNotFound struct {
	ID string
}

// Error returns the error message
func (e PolicyNotFound) Error() string {
	return fmt.Sprintf("policy with ID '%s' not found", e.ID)
}

// NewPolicyNotFound creates a new PolicyNotFound error instance
func NewPolicyNotFound(id string) PolicyNotFound {
	return PolicyNotFound{ID: id}
}

// fetchPoliciesFromOPA fetches all policies from the OPA service. Note that
// there is unfortunately no pagination support.
func fetchPoliciesFromOPA(opaSvcUrl string) (policies map[string]Policy, error error) {
	resp, err := http.Get(fmt.Sprintf("%s/%s", opaSvcUrl, BasePath))
	if err != nil {
		return nil, err
	}
	defer closeBody(resp.Body, &error)

	// The only two status codes we expect are 200 OK and 500 Internal Server
	// Error (see https://www.openpolicyagent.org/docs/latest/rest-api/#list-policies)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch policies from OPA service: %s", resp.Status)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	result := ListPoliciesResult{}
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
func fetchPolicyFromOPA(opaSvcUrl string, id string) (policy *Policy, error error) {
	resp, err := http.Get(fmt.Sprintf("%s/%s/%s", opaSvcUrl, BasePath, id))
	if err != nil {
		return nil, err
	}
	defer closeBody(resp.Body, &error)

	// The only three status codes we expect are 200 OK, 404 Not Found, and
	// 500 Internal Server Error (see https://www.openpolicyagent.org/docs/latest/rest-api/#get-a-policy)
	if resp.StatusCode != http.StatusOK {
		if resp.StatusCode == http.StatusNotFound {
			return nil, NewPolicyNotFound(id)
		} else {
			return nil, fmt.Errorf(
				"failed to fetch policy with id '%s': %s",
				id, resp.Status)
		}
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	result := GetPolicyResult{}
	err = json.Unmarshal(body, &result)
	if err != nil {
		return nil, err
	}
	return &result.Policy, nil
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
