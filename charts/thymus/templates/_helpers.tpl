{{/* vim: set filetype=mustache: */}}
{{/* Copyright (c) 2021 - for information on the respective copyright owner */}}
{{/* see the NOTICE file and/or the repository https://github.com/carbynestack/castor. */}}
{{/* SPDX-License-Identifier: Apache-2.0 */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "thymus.name" -}}
    {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "thymus.fullname" -}}
    {{- if .Values.fullnameOverride -}}
        {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
    {{- else -}}
        {{- $name := default .Chart.Name .Values.nameOverride -}}
        {{- if contains $name .Release.Name -}}
            {{- .Release.Name | trunc 63 | trimSuffix "-" -}}
        {{- else -}}
            {{- printf "%s-%s" $name .Release.Name | trunc 63 | trimSuffix "-" -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
