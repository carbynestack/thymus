// Copyright (c) 2024 - for information on the respective copyright owner
// see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
//
// SPDX-License-Identifier: Apache-2.0

package main

import (
	"log"
	"os"
	"policycatalogue/pkg/catalogue"
)

// getEnv fetches the environment variable or panics if not set
func getEnv(key string) string {
	value := os.Getenv(key)
	if value == "" {
		log.Panicf("%s environment variable is not set", key)
	}
	return value
}

func main() {
	opaSvcUrl := getEnv("OPA_SERVICE_URL")
	log.Println("Using OPA service URL:", opaSvcUrl)
	server := catalogue.NewServer(opaSvcUrl)
	err := server.Run()
	if err != nil {
		log.Panicf("error running server: %v", err)
	}
}
