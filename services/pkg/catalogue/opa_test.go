// Copyright (c) 2024 - for information on the respective copyright owner
// see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
//
// SPDX-License-Identifier: Apache-2.0

package catalogue

import (
	"encoding/json"
	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
	"net/http"
	"net/http/httptest"
)

var _ = Describe("OPA", func() {

	Describe("fetchPoliciesFromOPA", func() {
		It("should fetch all policies from OPA service", func() {
			mockServer := newMockServer(ListPoliciesResult{
				Policies: []Policy{
					{ID: "policy1", Code: "code1"},
					{ID: "policy2", Code: "code2"},
				},
			})
			defer mockServer.Close()

			policies, err := fetchPoliciesFromOPA(mockServer.URL)
			Expect(err).ToNot(HaveOccurred())
			Expect(policies).To(HaveLen(2))
			Expect(policies["policy1"].Code).To(Equal("code1"))
			Expect(policies["policy2"].Code).To(Equal("code2"))
		})
	})

	Describe("fetchPolicyFromOPA", func() {
		It("should fetch a single policy by ID from OPA service", func() {
			mockServer := newMockServer(GetPolicyResult{
				Policy: Policy{
					ID:   "policy1",
					Code: "code1",
					Ast:  "ast1",
				},
			})
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy1")
			Expect(err).ToNot(HaveOccurred())
			Expect(policy).ToNot(BeNil())
			Expect(policy.Code).To(Equal("code1"))
		})

		It("should return an error if policy is not found", func() {
			mockServer := httptest.NewServer(http.HandlerFunc(
				func(w http.ResponseWriter, r *http.Request) {
					w.WriteHeader(http.StatusNotFound)
				}))
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy2")
			Expect(err).To(HaveOccurred())
			Expect(policy).To(BeNil())
		})
	})

})

func newMockServer(result interface{}) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		err := json.NewEncoder(w).Encode(result)
		Expect(err).ToNot(HaveOccurred())
	}))
}
