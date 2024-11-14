#!/bin/bash

#
# Copyright (c) 2024 - for information on the respective copyright owner
# see the NOTICE file and/or the repository https://github.com/carbynestack/thymus.
#
# SPDX-License-Identifier: Apache-2.0
#

# This script creates a user in Kratos and verifies the user's email address.
#
# The script expects the following environment variables to be set:
# - MAILSLURPER_ADDRESS: The address of the MailSlurper instance.
# - KRATOS_ADMIN_SERVICE_ADDRESS: The address of the Kratos admin service instance.
# - KRATOS_PUBLIC_SERVICE_ADDRESS: The address of the Kratos public service instance.
# - RETRY_PERIOD: The time to wait between retries when polling for the verification code. Default is 1 second.
# - RETRIES: The number of times to retry getting the verification code. Default is 10.

RETRY_PERIOD=${RETRY_PERIOD:-1}
RETRIES=${RETRIES:-10}

if [ -z "${MAILSLURPER_ADDRESS}" ]; then
  echo "Error: MAILSLURPER_ADDRESS environment variable is not set."
  exit 1
fi

if [ -z "${KRATOS_ADMIN_SERVICE_ADDRESS}" ]; then
  echo "Error: KRATOS_ADMIN_SERVICE_ADDRESS environment variable is not set."
  exit 1
fi

if [ -z "${KRATOS_PUBLIC_SERVICE_ADDRESS}" ]; then
  echo "Error: KRATOS_PUBLIC_SERVICE_ADDRESS environment variable is not set."
  exit 1
fi

# Checks if the last command failed and exits the script if it did.
exitOnError() {
  STATUS_CODE=$?
  if [ $STATUS_CODE -ne 0 ]; then
    echo "$1 Exit Code: $STATUS_CODE"
    exit 1
  fi
}

# Gets the verification code from the email sent by Kratos.
getVerificationCode() {
  email=$1
  for i in $(seq 1 "${RETRIES}"); do
    code=$(curl -X GET -sf -H 'Content-Type: application/JSON' \
      "http://${MAILSLURPER_ADDRESS}/mail?to=${email}\&order=desc" | \
      jq -r '.mailItems[0].body | capture("code: (?<code>\\w+)").code')
    exitOnError "Failed to get verification code for user: $email."
    if [[ -n "$code" ]]; then
      echo "$code"
      return
    fi
    sleep "$((i*RETRY_PERIOD))"
  done
  echo "Failed to get verification code for user: $email."
  exit 1
}

# Extracts the email from the user credentials file.
email=$(jq -r '.traits.email' < /user-credentials/data.json)

echo "Creating user: ${email}"
curl -X POST -vf -H 'Content-Type: application/json' -d @/user-credentials/data.json "http://${KRATOS_ADMIN_SERVICE_ADDRESS}/admin/identities"
exitOnError "Failed to create user: ${email}."

echo "Verifying user: ${email}"
flowID=$(curl -X GET -sf -H 'Content-Type: application/JSON' \
  "http://${KRATOS_PUBLIC_SERVICE_ADDRESS}/self-service/verification/api" | \
  jq -r '.id')
exitOnError "Failed to create verification flow for user: ${email}."

curl -X POST -sf -H 'Content-Type: application/JSON' \
  "http://${KRATOS_PUBLIC_SERVICE_ADDRESS}/self-service/verification?flow=${flowID}" \
  --data "{\"method\": \"code\", \"email\": \"${email}\"}"
exitOnError "Failed to initiate verification flow for user: ${email}."

code=$(getVerificationCode "${email}")
curl -X POST -sf -H 'Content-Type: application/JSON' \
  "http://${KRATOS_PUBLIC_SERVICE_ADDRESS}/self-service/verification?flow=${flowID}" \
  --data "{\"method\": \"code\", \"code\": \"$code\"}"
exitOnError "Failed finalize verification for user: ${email}."

echo "User ${email} successfully created"
