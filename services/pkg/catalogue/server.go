// Copyright (c) 2024 - for information on the respective copyright owner
// see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
//
// SPDX-License-Identifier: Apache-2.0

package catalogue

import (
	"fmt"
	"github.com/gin-gonic/gin"
	healthcheck "github.com/tavsec/gin-healthcheck"
	"github.com/tavsec/gin-healthcheck/checks"
	"github.com/tavsec/gin-healthcheck/config"
	"net/http"
	"strings"
)

// Server represents the policy catalogue server instance
type Server struct {
	opaSvcUrl string
}

// NewServer creates a new server instance with the given OPA service URL
func NewServer(opaSvcUrl string) *Server {
	return &Server{opaSvcUrl: opaSvcUrl}
}

// encodeID encodes the given OPA policy ID by stripping the prefix and replacing "/" with ":"
func encodeID(id string) string {
	return strings.Replace(id[len("stackable/bundles/"):], "/", ":", -1)
}

// decodeID decodes the given ID by replacing ":" with "/" and prepending the prefix
func decodeID(id string) string {
	return fmt.Sprintf("stackable/bundles/%s", strings.Replace(id, ":", "/", -1))
}

// handleGetPolicies handles the GET /policies endpoint
func (s *Server) handleGetPolicies(c *gin.Context) {
	policies, err := fetchPoliciesFromOPA(s.opaSvcUrl)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Create array of stripped policy IDs
	ids := make([]string, 0)
	for id := range policies {
		ids = append(ids, encodeID(id))
	}

	c.JSON(http.StatusOK, ids)
}

// handleGetPolicyByID handles the GET /policies/:id endpoint
func (s *Server) handleGetPolicyByID(c *gin.Context) {
	id := c.Param("id")

	policy, err := fetchPolicyFromOPA(s.opaSvcUrl, decodeID(id))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	if policy == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "policy not found"})
		return
	}

	switch c.GetHeader("Accept") {
	case "application/json":
		c.JSON(http.StatusOK, policy)
	case "text/plain", "":
		c.String(http.StatusOK, policy.Code)
	default:
		c.JSON(http.StatusUnsupportedMediaType, gin.H{"error": "unsupported content type"})
	}
}

// setupRouter sets up the server's router
func (s *Server) setupRouter() *gin.Engine {
	r := gin.Default()
	r.Use(gin.Logger())

	r.GET("/policies", s.handleGetPolicies)
	r.GET("/policies/:id", s.handleGetPolicyByID)

	return r
}

// setupHealthCheck sets up the server's health check endpoint
func (s *Server) setupHealthCheck(r *gin.Engine) error {
	cfg := config.DefaultConfig()
	cfg.HealthPath = "/health"
	return healthcheck.New(r, cfg, []checks.Check{})
}

// Run starts the server and listens on port 8080
func (s *Server) Run() error {
	r := s.setupRouter()
	err := s.setupHealthCheck(r)
	if err != nil {
		return err
	}
	return r.Run(":8080")
}
