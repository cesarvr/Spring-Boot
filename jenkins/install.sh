#!/bin/sh

oc new-build nodejs~https://github.com/cesarvr/Spring-Boot --name=build-$1 --strategy=pipeline
oc set env bc/my-application https_proxy=http://vsdbahlprxy1:8080 APPLICATION_NAME=$1 PROXY=vsdbahlprxy1