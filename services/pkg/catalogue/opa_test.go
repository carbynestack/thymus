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
			mockServer := newMockServer(Result{
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
			mockServer := newMockServer(Result{
				Policies: []Policy{
					{ID: "policy1", Code: "code1"},
				},
			})
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy1")
			Expect(err).ToNot(HaveOccurred())
			Expect(policy).ToNot(BeNil())
			Expect(policy.Code).To(Equal("code1"))
		})

		It("should return nil if policy is not found", func() {
			mockServer := newMockServer(Result{
				Policies: []Policy{
					{ID: "policy1", Code: "code1"},
				},
			})
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy2")
			Expect(err).ToNot(HaveOccurred())
			Expect(policy).To(BeNil())
		})
	})

})

func newMockServer(result Result) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		Expect(r.URL.Path).To(Equal("/v1/policies"))
		w.WriteHeader(http.StatusOK)
		err := json.NewEncoder(w).Encode(result)
		Expect(err).ToNot(HaveOccurred())
	}))
}
