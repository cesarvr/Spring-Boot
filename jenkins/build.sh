#!/bin/bash

oc new-build redhat-openjdk18-openshift:1.2 --name=$1 --binary=true -l app=$1 || true &&
oc create dc $1 --image=$(oc get is $1 -o=jsonpath='{.status.dockerImageRepository}') || true &&
oc set triggers dc/$1 --from-image=$(oc get is $1 -o=jsonpath='{.status.dockerImageRepository}'):latest || true &&
oc expose dc $1 --port=8080 -l app=$1 || true &&
oc expose svc $1 -l app=$1 || true &&
oc label dc $1 app=$1 || true  && #Add our DC to a common label app

# Set an environment variable into the Deployment Config, this way all container inherith this value
# This APPLICATION_NAME will define the application name in the resources/application.properties file.
oc set env dc/$1 APPLICATION_NAME=$1 || true 