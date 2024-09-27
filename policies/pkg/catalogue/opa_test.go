// Copyright (c) 2024 - for information on the respective copyright owner
// see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
//
// SPDX-License-Identifier: Apache-2.0

package catalogue

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
)

func TestOPA(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "OPA Suite")
}

var _ = Describe("OPA", func() {

	Describe("fetchPoliciesFromOPA", func() {
		It("should fetch all policies from OPA service", func() {
			mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				Expect(r.URL.Path).To(Equal("/v1/policies"))
				w.WriteHeader(http.StatusOK)
				err := json.NewEncoder(w).Encode(Result{
					Policies: []Policy{
						{ID: "policy1", Code: "code1"},
						{ID: "policy2", Code: "code2"},
					},
				})
				if err != nil {
					return
				}
			}))
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
			mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				Expect(r.URL.Path).To(Equal("/v1/policies"))
				w.WriteHeader(http.StatusOK)
				err := json.NewEncoder(w).Encode(Result{
					Policies: []Policy{
						{ID: "policy1", Code: "code1"},
					},
				})
				if err != nil {
					return
				}
			}))
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy1")
			Expect(err).ToNot(HaveOccurred())
			Expect(policy).ToNot(BeNil())
			Expect(policy.Code).To(Equal("code1"))
		})

		It("should return nil if policy is not found", func() {
			mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				Expect(r.URL.Path).To(Equal("/v1/policies"))
				w.WriteHeader(http.StatusOK)
				err := json.NewEncoder(w).Encode(Result{
					Policies: []Policy{
						{ID: "policy1", Code: "code1"},
					},
				})
				if err != nil {
					return
				}
			}))
			defer mockServer.Close()

			policy, err := fetchPolicyFromOPA(mockServer.URL, "policy2")
			Expect(err).ToNot(HaveOccurred())
			Expect(policy).To(BeNil())
		})
	})

})
