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
	"strings"
)

func setupMockServer(faulty bool) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if faulty {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
		if r.URL.Path == "/v1/policies" {
			w.WriteHeader(http.StatusOK)
			err := json.NewEncoder(w).Encode(ListPoliciesResult{
				Policies: []Policy{
					{ID: "stackable/bundles/policy1", Code: "code1"},
				},
			})
			if err != nil {
				Fail(err.Error())
			}
		} else if strings.HasPrefix(r.URL.Path, "/v1/policies/stackable/bundles") {
			if strings.HasSuffix(r.URL.Path, "/policy1") {
				w.WriteHeader(http.StatusOK)
				err := json.NewEncoder(w).Encode(GetPolicyResult{
					Policy: Policy{
						ID:   "stackable/bundles/policy1",
						Code: "code1",
						Ast:  "ast1",
					},
				})
				if err != nil {
					Fail(err.Error())
				}
			} else {
				w.WriteHeader(http.StatusNotFound)
			}
		}
	}))
}

var _ = Describe("Server with a faulty OPA service", func() {

	var (
		mockServer *httptest.Server
		server     *Server
	)

	BeforeEach(func() {
		mockServer = setupMockServer(true)
		server = NewServer(mockServer.URL)
	})

	AfterEach(func() {
		mockServer.Close()
	})

	Describe("handleGetPolicies", func() {
		It("should return an error", func() {
			req, _ := http.NewRequest("GET", "/policies", nil)
			resp := httptest.NewRecorder()
			router := server.setupRouter()
			router.ServeHTTP(resp, req)

			Expect(resp.Code).To(Equal(http.StatusInternalServerError))
		})
	})
})

var _ = Describe("Server with a healthy OPA service", func() {

	var (
		mockServer *httptest.Server
		server     *Server
	)

	BeforeEach(func() {
		mockServer = setupMockServer(false)
		server = NewServer(mockServer.URL)
	})

	AfterEach(func() {
		mockServer.Close()
	})

	Describe("Run", func() {
		It("should start a healthy server without errors", func() {
			go func() {
				err := server.Run()
				Expect(err).ToNot(HaveOccurred())
			}()
			resp, err := http.Get("http://localhost:8080/health")
			Expect(err).ToNot(HaveOccurred())
			Expect(resp.StatusCode).To(Equal(http.StatusOK))
		})
	})

	Describe("handleGetPolicies", func() {
		It("should return a list of policy IDs", func() {
			req, _ := http.NewRequest("GET", "/policies", nil)
			req.Header.Add("Accept", "application/json")
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
		When("Accept header is application/json", func() {
			It("should return policy details for a valid ID", func() {
				req, _ := http.NewRequest("GET", "/policies/policy1", nil)
				req.Header.Add("Accept", "application/json")
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

		When("accept header is text/plain", func() {
			It("should return policy code for a valid ID", func() {
				req, _ := http.NewRequest("GET", "/policies/policy1", nil)
				req.Header.Add("Accept", "text/plain")
				resp := httptest.NewRecorder()
				router := server.setupRouter()
				router.ServeHTTP(resp, req)

				Expect(resp.Code).To(Equal(http.StatusOK))
				Expect(resp.Body.String()).To(Equal("code1"))
			})
		})

		When("policy is not found", func() {
			It("should return 404", func() {
				req, _ := http.NewRequest("GET", "/policies/policy2", nil)
				req.Header.Add("Accept", "application/json")
				resp := httptest.NewRecorder()
				router := server.setupRouter()
				router.ServeHTTP(resp, req)

				Expect(resp.Code).To(Equal(http.StatusNotFound))
			})
		})
	})
})
