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
	"testing"
)

func TestServer(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Server Suite")
}

func setupMockServer() *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		Expect(r.URL.Path).To(Equal("/v1/policies"))
		w.WriteHeader(http.StatusOK)
		err := json.NewEncoder(w).Encode(Result{
			Policies: []Policy{
				{ID: "stackable/bundles/policy1", Code: "code1"},
			},
		})
		if err != nil {
			return
		}
	}))
}

var _ = Describe("Server", func() {

	var (
		mockServer *httptest.Server
		server     *Server
	)

	BeforeEach(func() {
		mockServer = setupMockServer()
		server = NewServer(mockServer.URL)
	})

	AfterEach(func() {
		mockServer.Close()
	})

	Describe("handleGetPolicies", func() {
		It("should return a list of policy IDs", func() {
			req, _ := http.NewRequest("GET", "/policies", nil)
			resp := httptest.NewRecorder()
			router := server.setupRouter()
			router.ServeHTTP(resp, req)

			Expect(resp.Code).To(Equal(http.StatusOK))
			var ids []string
			err := json.Unmarshal(resp.Body.Bytes(), &ids)
			Expect(err).ToNot(HaveOccurred())
			Expect(ids).To(ContainElement("policy1"))
		})
	})

	Describe("handleGetPolicyByID", func() {
		When("Content-Type is application/json", func() {
			It("should return policy details for a valid ID", func() {
				req, _ := http.NewRequest("GET", "/policies/policy1", nil)
				req.Header.Add("Content-Type", "application/json")
				resp := httptest.NewRecorder()
				router := server.setupRouter()
				router.ServeHTTP(resp, req)

				Expect(resp.Code).To(Equal(http.StatusOK))
				var policy Policy
				err := json.Unmarshal(resp.Body.Bytes(), &policy)
				Expect(err).ToNot(HaveOccurred())
				Expect(policy.Code).To(Equal("code1"))
			})
		})

		When("Content-Type is text/plain", func() {
			It("should return policy code for a valid ID", func() {
				req, _ := http.NewRequest("GET", "/policies/policy1", nil)
				req.Header.Add("Content-Type", "text/plain")
				resp := httptest.NewRecorder()
				router := server.setupRouter()
				router.ServeHTTP(resp, req)

				Expect(resp.Code).To(Equal(http.StatusOK))
				Expect(resp.Body.String()).To(Equal("code1"))
			})
		})

		It("should return 404 if policy is not found", func() {
			req, _ := http.NewRequest("GET", "/policies/policy2", nil)
			resp := httptest.NewRecorder()
			router := server.setupRouter()
			router.ServeHTTP(resp, req)

			Expect(resp.Code).To(Equal(http.StatusNotFound))
		})
	})
})
