#!/bin/python

import subprocess
from os import listdir
import sys 

def readTemplates(file, arguments):
	content = open(file, "r").read()
        for name in arguments.keys():
            content =  content.replace("%" + name + "%", arguments[name])
        return content

def parseBuildTemplate(path, arguments):
	templates = listdir(path)
        templates = map(lambda template: path + "/" + template, templates)
	return map(lambda template: readTemplates(template, arguments), templates)


def loadTemplates(arguments):
    if "from" not in arguments:
        patches_folder  = "patches"
    else:
        patches_folder  = arguments["from"]

    print "Patching Deployment"
    return parseBuildTemplate(patches_folder, arguments) 


def getUserArguments():
    USAGE = "Usage: deploy.py param_name=<value1> param_name=<value2>"
    args = sys.argv[1:]

    if not args:
        sys.exit(USAGE)

    ap = [pair.split("=") for pair in args]
    ap = map(lambda n: (n[0], n[1]), ap) 
    ret = dict(ap)
    
    return ret 



def buildComponents(parsedTemplates, arguments):
 ns = arguments['project']
 cmd = ["oc", "apply", "-n", ns, "-f", "-"]

 for template in parsedTemplates: 
    ocProcess = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    ocProcess.communicate(template)
    ocProcess.wait()

def applyPatches(patchTemplates, arguments):
    name = arguments['name']
    ns   = arguments['project']

    for template in patchTemplates: 
        cmd  = ["oc", "patch", "dc", name, "-n", ns, "--patch", template]
        oc_proc = subprocess.Popen(cmd,  stdin=subprocess.PIPE)
        oc_proc.wait()

arguments = getUserArguments()

arguments['from'] = "templates"
templates = loadTemplates(arguments)
buildComponents(templates, arguments)

arguments['from'] = "patches"
templates = loadTemplates(arguments)
applyPatches(templates, arguments)
