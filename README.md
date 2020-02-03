
Table of contents
=================

<!--ts-->
   * [Getting Your Code Into Jenkins](#getting-your-code-into-jenkins)
   * [Local Development](#local)
<!--te-->



## Getting Started

### Tools
To make your life easier with Openshift you will need some of the tools that are available in Linux, if you are stuck with Windows then there is some ways to get the Linux tools the first option is to use the [Linux virtualization via Windows WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) which is basically Linux [user-space](https://en.wikipedia.org/wiki/User_space) emulated by Windows System calls, this is good enough to do heavy development. Your second option is to use [Cmder](https://cmder.net/) which brings the Linux feeling to your Windows *day-to-day* and include tools such [Cygwin](https://en.wikipedia.org/wiki/Cygwin) (Gnu/Unix popular tools ported to Windows) and [Git](https://en.wikipedia.org/wiki/Git) client.

![](https://cmder.net/img/main.png)

> Cmder UI

### Openshift Client

Once you have your *Unix-like* setup, the next step is to the Openshift client which is basically the best way to get Openshift to do stuff for you, to install this tool just get the binary ([Windows](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-windows.zip), [Linux](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz)) and make it available in your PATH:

```sh
set "PATH=%PATH%;<your-oc-cli-folder>\oc-cli"
```


<a name="start"/>

## Getting Your Code Into Jenkins

A quick way to get your Java Spring Boot Project to Openshift is reusing this convenient [installation script](https://github.com/cesarvr/Spring-Boot/blob/master/jenkins/install.sh) provided in this project, but first you need to login into your account:

```sh
#Login into Openshift
oc login
# Authentication required for ...

# Create a project
oc new-project <your-project>

# Go to your project
oc project <your-project>
```

Once you have logged-in and setup your project you can create the pipeline build, but first you need the *HTTP* URL of your git repository then pass it to the ``install`` script like this:

```sh
sh jenkins\install.sh <micro-service-name> <git-HTTP-url-to-your-code>

#Example
sh jenkins\install.sh service-b https://github.com/cesarvr/Spring-Boot.git
```

This will create a Openshift pipeline build which automatically do this:

- Creates (if is doesn't exist) an instance of Jenkins in your namespace/project.  
- Add this Jenkins Pipeline Script (The ``Jenkinsfile`` included in the root directory of this project).


![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/service-a.PNG)


### The Pipeline Is There Now What ?

Once the pipeline is created it will create the [Openshift components](https://github.com/cesarvr/Openshift) ([BuildConfig](#), Deployment Configuration, Service and Router) to deploy your Spring Boot application. The code to create this components is stored in the root folder Jenkins folder/[build.sh](https://github.com/cesarvr/Spring-Boot/blob/master/jenkins/build.sh) and is invoked by the [Jenkinsfile](https://github.com/cesarvr/Spring-Boot/blob/master/Jenkinsfile#L32) as part of the build process:

```groovy
  steps {
    echo "Creating Openshift Objects"
    sh "echo creating objects for ${appName} && chmod +x ./jenkins/build.sh && ./jenkins/build.sh ${appName}"
  }
```

> The Jenkinsfile is the place that you should start customizing to fit your particular case.





<a name="local"/>

## Local Development

One of the best ways to get a feeling of how your services behave in Openshift is to deploy your applications there, here I provide a ``script`` to create a prototypical infrastructure to deploy a micro-service, to create this you should do:

```sh
 sh jenkins\build.sh my-java-app
```

> This creates the Openshift components to deploy Spring Boot applications.

Now we just need to send our self-bootable-server-jar there, we can do this by running the following command:

First generate the JAR:

```sh
mvn package
```
> Before pushing *JAR binaries* to Openshift just keep in mind that the supported OpenJDK version is ``"1.8.0_161``.

Then push the JAR to the [Build Configuration](#) by doing:

```sh
 oc start-build bc/my-java-app --from-file=target\spring-boot-0.0.1-SNAPSHOT.jar --follow
```

> If this command finish successfully, it means that there is an [image](#) in the cluster with your application.

Next step is to deploy this [image](#) you can do this by doing:

```sh
oc rollout latest dc/my-java-app
```

![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/deploy.PNG)


> This take the container with your application and creates an instance in one of the ``worker-nodes``.

To access the application you need to retrieve the URL:

```sh
oc get routes  my-java-app -o=jsonpath='{.spec.host}'
# my-java-service-url
```

![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/url.PNG)
> Past the URL in your browser and you should be able to see your application.

### Re-Deploy

The creation process with the ``build.sh`` should be done once, to re-deploy new changes you can do this:

```sh
mvn package
oc start-build bc/my-java-app --from-file=target\spring-boot-0.0.1-SNAPSHOT.jar --follow
```
> Your changes should be now deployed.


### Delegating Source Code Compilation To Openshift

Sometimes pushing a binary can be problematic because:

- You have a different Java version than the container.
- You don't have Maven installed.


In those cases you can send your Spring Boot source code (only Maven supported) to the [Build Configuration](#) by doing this:

```sh
oc start-build bc/my-java-app --from-file=. --follow
```

Everything from here is the same as the binary version:

```sh
oc rollout latest dc/my-java-app
```

### Troubleshooting Problems

### Logs

- If something wrong happens while deploying (like ``oc rollout latest``) you can check the logs of the container by doing:

```sh
oc get pod | grep my-java-app
# my-java-app-1-build                 0/1       Completed   0          15m
# my-java-app-2-d6zs4                 1/1       Running     0          8m
```

We see here two [container](#appendix) the one with suffix ``build`` means that this container was in charge of the [building process](#) (putting your JAR in place, configuration, etc.). The one with suffix ``d6zs4`` (this is random) is the one holding your application, so if something is wrong at runtime you should look for the logs there, for example:

```sh
oc log my-java-app-2-d6zs4

log is DEPRECATED and will be removed in a future version. Use logs instead.
Starting the Java application using /opt/run-java/run-java.sh ...
exec java -javaagent:/opt/jolokia/jolokia.jar=config=/opt/jolokia/etc/jolokia.pro...
No access restrictor found, access to any MBean is allowed
Jolokia: Agent started with URL https://10.130.3.218:8778/jolokia/

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.2.RELEASE)
```

### Debug

If the pod is crashing continuously you won't have time to see the ``logs`` of the pod, in that case you can use the ``oc-debug`` command to *revive* crashed containers.

```sh
oc get pod | grep my-java-app
# my-java-app-1-build                0/1       Completed   0          15m
# my-java-app-2-x664                 1/1       Crash       0          8m
```

```sh
oc debug my-java-app-2-x664
# /bin/sh
```

This will give you a temporary shell inside the container there you can try to execute manually the JAR and see reproduce the crashing behavior.

## Appendix

<a name="appendix-1"/>

In reality Openshift uses an abstraction called **pod** whose purpose is to facilitate the deployment of one or many containers and made them behave as a single entity (or a single container). [For more information about pods](https://kubernetes.io/docs/concepts/workloads/pods/pod/)


[A hard way to run Jenkins](https://github.com/cesarvr/Spring-Boot/blob/master/OLD_README.md)
