#!/bin/sh

oc new-build nodejs~$2 --name=build-$1 --strategy=pipeline
oc set env bc/build-$1 APPLICATION_NAME=$1 #PROXY=your-proxy
