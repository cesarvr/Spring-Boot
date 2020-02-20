Table of contents
=================

<!--ts-->
   * [Getting Your Code Into Jenkins](#getting-your-code-into-jenkins)
   * [Local Development](#local-development)
   * [Debugging A Container (Running On OpenShift)](#debugging-a-container)
   * [Watch The Logs](#watching-the-logs)
   * [Zipkin Instrumentation](#zipkin-instrumentation)
<!--te-->



## Getting A Unix Environment

To get the most of the Openshift client (``oc-client``) you need some tools available for Linux, if you are stuck with Windows you have two options:

- One is to use the [Linux virtualization via Windows WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) which is basically Linux [user-space](https://en.wikipedia.org/wiki/User_space) emulated by Windows System calls.

- Your second option is to use [Cmder](https://github.com/cmderdev/cmder/releases/download/v1.3.14/cmder.zip) which brings the Linux feeling to your Windows *day-to-day* and include tools such [Cygwin](https://en.wikipedia.org/wiki/Cygwin) (Gnu/Unix popular tools ported to Windows), [Git](https://en.wikipedia.org/wiki/Git), tar, etc.

![](https://cmder.net/img/main.png)

> Cmder UI

### Openshift Client

Once you have your *Unix-like* setup you need to get the ``oc-client``, this will allow you to control Openshift from your command-line. You can get the binary for ([Windows here](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-windows.zip) or [Linux](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz)) decompress and add it to your PATH:

```sh
# Linux
export PATH=$PATH:<your-decompressed-oc-client-folder-location>\

# Windows
set "PATH=%PATH%;<your-decompressed-oc-client-folder-location>\"
```

<a name="start"/>

## Getting Your Code Into Jenkins

This Java Spring Boot Project includes a [pipeline installation script](https://github.com/cesarvr/Spring-Boot/blob/master/jenkins/install.sh) that will setup a quick and simple Jenkins Pipeline using Openshift in build pipeline strategy, before using it make sure you are logged in and inside your project:

```sh
#Login into Openshift
oc login
# Authentication required for ...

# Create a project
oc new-project <your-project>

# Go to your project
oc project <your-project>
```

Now you can create the pipeline like this:

```sh
sh jenkins\install.sh <micro-service-name> <git-HTTP-url-to-your-code>

#Example
sh jenkins\install.sh service-b https://github.com/cesarvr/Spring-Boot.git
```

This will create a Openshift pipeline build which automatically do this:

- Creates (if there is none) an instance of Jenkins in your namespace/project.  
- Creates a Job in this instance using the ``Jenkinsfile`` included in the root directory of this project.

![Full process](https://github.com/cesarvr/Spring-Boot/blob/master/docs/cicd.gif?raw=true)

> If there is a Jenkins already deployed in your in the namespace, it will reuse that one.

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

### Watching The Logs

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

### Debugging A Container (Running On OpenShift)

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



<a name="tracing"/>

## Zipkin Instrumentation

This project also includes [Zipkin](https://zipkin.io/) instrumentation provided by [Spring Boot Sleuth](https://spring.io/projects/spring-cloud-sleuth) framework which makes instrumentation transparent to your business logic.

To configure the instrumentation you can edit the ``application.properties`` in your resource folder:

```properties
spring.zipkin.baseUrl = https://my-zipkin-server/
spring.sleuth.sampler.probability = 1
spring.sleuth.enabled = true

spring.application.name = hello-ping-1
```

- ``spring.zipkin.baseUrl``
  - Is the URL for the [Zipkin server](#).
- ``sampler.probability``
  - The value ``1`` tells **sleuth** to [send the traces](https://cloud.spring.io/spring-cloud-sleuth/2.0.x/multi/multi__sampling.html#_sampling_in_spring_cloud_sleuth) to the Zipkin server, while ``0`` just logs the results.
- ``application.name``
  - This the name that will appear in the traces.

At the moment this example publish the traces to [this Zipkin server](#), you can do there to observe your service behavior.

### How Do I Test This

To see how it working, you can deploy two services using this particular version:

```sh
  sh jenkins\install.sh service-a https://github.com/cesarvr/Spring-Boot.git
  sh jenkins\install.sh service-b https://github.com/cesarvr/Spring-Boot.git
```

> This will deploy two Spring Boot services ``service-a`` and ``service-b``.

To test the instrumentation I have added two endpoints:
  - ``ping`` Which make a call to another microservice ``pong`` endpoint (specified by the variable ``PONG_ENDPOINT``) and append the response obtaining (hopefully) ``Ping! Pong!``.
  - ``pong`` Which just returns ``Pong!``

We need now to configure the ``PONG_ENDPOINT`` in both services and change the ``application.name`` (at the moment they should share the same ``application.name``) in one of them so we can identify the service on Zipkin Dashboard later:

#### Pointing To The Endpoint

First the URL's for the routers of each service:

```sh
 oc get route
 # service-a   service-a-my-project.apps.xx.com    service-a   8080                      None
 # service-b   service-b-my-project.apps.xx.com    service-b   8080                      None
```

Then setup the ``PONG_ENDPOINT`` of each service to point to its neighbor:

```sh
 oc set env dc/service-b PONG_ENDPOINT=http://service-a-my-project.apps.xx.com
 oc set env dc/service-a PONG_ENDPOINT=http://service-b-my-project.apps.xx.com
```

We should have this graph:

![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/zipkin.PNG)

> When you access the ``/ping`` endpoint to any of this two service it will ask the other service for his ``/pong`` endpoint and will concatenate the returned string and return it back.


#### Changing The Name

One thing that is not right yet is that both services are using the same source code although they share the same ``application.properties``. To fix this (assuming that you are running this project locally) you just need to change this value in the ``properties`` file:

```sh
  application.name = service-b  # from service-a
```

```sh
oc get bc

# NAME            TYPE        FROM         LATEST
# service-a       Source      Binary       2
# service-b       Source      Binary       2

oc start-build bc/service-b --from-file=. --follow
oc rollout latest dc/service-b
```

> In this case we changed the name to service-b and we rebuild the image again.

Generate some traffic:

```sh
curl http://service-a-my-project.apps.xx.com/ping
#Ping! Pong!
curl http://service-a-my-project.apps.xx.com/ping
#Ping! Pong!
curl http://service-a-my-project.apps.xx.com/ping
#Ping! Pong!
```

And now you can visit your traces here:

![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/tracing.PNG)
> Global view

![](https://raw.githubusercontent.com/cesarvr/Spring-Boot/master/docs/tracing-inside.PNG)
> Debugging a trace

## Appendix

<a name="appendix-1"/>

In reality Openshift uses an abstraction called **pod** whose purpose is to facilitate the deployment of one or many containers and made them behave as a single entity (or a single container). [For more information about pods](https://kubernetes.io/docs/concepts/workloads/pods/pod/)


[A hard way to run Jenkins](https://github.com/cesarvr/Spring-Boot/blob/master/OLD_README.md)
