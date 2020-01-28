#!/bin/sh

oc new-build redhat-openjdk18-openshift:1.2 --name=$1 --binary=true -l app=$1 || true &&
oc create dc $1 --image=$(oc get is $1 -o=jsonpath='{.status.dockerImageRepository}') || true &&
oc expose dc $1 --port=8080 -l app=$1 || true &&
oc expose svc $1 -l app=$1 || true
