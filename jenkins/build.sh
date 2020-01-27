#!/bin/sh

oc new-build --name=$1 --binary=true &&
oc create dc $1 --image=$1 &&
oc expose dc $1 --port=8080 &&
oc expose svc $1
