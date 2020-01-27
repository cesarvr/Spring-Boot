#!/bin/sh

oc new-build redhat-openjdk18-openshift:1.2 --name=$1 --binary=true || true &&
oc create dc $1 --image=$(oc get is $1 -o=jsonpath='{.status.dockerImageRepository}') || true &&
oc expose dc $1 --port=8080 || true &&
oc expose svc $1 || true
