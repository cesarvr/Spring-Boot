
Table of contents
=================

<!--ts-->
   * [Getting Your Code Into Jenkins](#start)
   * [Local Development](#local)
   * [Deploying WAR's](#openshift)
   * [Configuring Continuous Integration](#continous)
   * [Openshift Pipeline Integration](#faster)
<!--te-->



## Getting Started

#### Tools
To make your life easier with Openshift you will need some of the tools that are available in Linux, if you are stuck with Windows then there is some ways to get the Linux tools the first option is to use the [Linux virtualization via Windows WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) which is basically Linux [user-space](https://en.wikipedia.org/wiki/User_space) emulated by Windows System calls, this is good enough to do heavy development. Your second option is to use [Cmder](https://cmder.net/) which brings the Linux feeling to your Windows *day-to-day* and include tools such [Cygwin](https://en.wikipedia.org/wiki/Cygwin) (Gnu/Unix popular tools ported to Windows) and [Git](https://en.wikipedia.org/wiki/Git) client.

![](https://cmder.net/img/main.png)

> Cmder UI

#### Openshift Client

Once you have your *Unix-like* setup, the next step is to the Openshift client which is basically the best way to get Openshift to do stuff for you, to install this tool just get the binary ([Windows](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-windows.zip), [Linux](https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz)) and make it avaialable in your PATH: 

```sh
set "PATH=%PATH%;<your-oc-cli-folder>\oc-cli\"
``` 


<a name="start"/>

## Getting Your Code Into Jenkins

A quick way to get your Java Spring Boot Project to Openshift is reusing this convinient *installation script* provided in this project, but first you need Openshift client  then you need to login with your account: 

```sh
#Login into Openshift
oc login 
# Authentication required for ... 

# Create a project
oc new-project <your-project>

# Go to your project
oc project <your-project>
```

Once you have logged-in and setup your project you can create the pipeline build by doing: 

```sh
sh jenkins\install.sh <micro-service-name> <git-url-for-your-code>
sh jenkins\install.sh service-b http://gogs-test-cesar-3.apps.rhos.agriculture.gov.ie/cesarv/java-microservice
```

This will create a Openshift pipeline build which automatically do this: 

- Creates (if is doesn't exist) an instance of Jenkins in your namespace/project.  
- Add this Jenkins Pipeline Script (The ``Jenkinsfile`` included in the root directory of this project). 


![](http://gogs-test-cesar-3.apps.rhos.agriculture.gov.ie/cesarv/java-microservice/raw/master/docs/service-a.PNG)


### The Pipeline Is There Now What ?

Once the pipeline is created it will create the [Openshift components](https://github.com/cesarvr/Openshift) (BuildConfig, Deployment Config, Service and Router) to deploy your Spring Boot application. The code to create this components is stored in the root folder jenkins folder/[build.sh](https://github.com/cesarvr/Spring-Boot/blob/master/jenkins/build.sh) and is invoked by the [Jenkinsfile](https://github.com/cesarvr/Spring-Boot/blob/master/Jenkinsfile#L32) as part of the build process: 

```groovy
  steps {
    echo "Creating Openshift Objects"
    sh "echo creating objects for ${appName} && chmod +x ./jenkins/build.sh && ./jenkins/build.sh ${appName}"
  }
```

> The Jenkinsfile is the place that you should start customizing to fit your particular case.

<a name="openshift"/>

# Spring Boot in Openshift

This example project is for people that want to start playing with Spring Boot in Openshift.

Its based in the hello world located in the [Spring website](https://spring.io/guides/gs/spring-boot/), I just modify the ```pom.xml``` so the code can be deploy using the Wildfly/Openshift template.




<a name="local"/>

## Local Development

One of the best ways to get a feeling of how your services behave in Openshift is to deploy your applications there, here I provide a ``script`` to create a prototypical infraestructure to deploy a micro-service, to create this you should do: 

```sh
 sh jenkins\build.sh my-java-app
``` 

> This creates the Openshift components to deploy Spring Boot applications. 

Now we just need to send our self-bootable-server-jar there, we can do this by running the follwing command: 

First generate the JAR: 

```sh
mvn package # 
```

Then push the JAR to the BuildConfiguration by doing: 

```sh
 oc start-build bc/my-java-app --from-file=target\spring-boot-0.0.1-SNAPSHOT.jar --follow
```

> If this command finish succesfully, it means that there is an image in the cluster with your application. 


Next step is to deploy this image you can do this by doing: 

```sh
oc rollout latest dc/my-java-app
```

> This take the container with your application and creates an instance in one of the ``worker-nodes``. 

To access the application you need to retrieve the URL: 

```sh

#
oc get routes  my-java-app -o=jsonpath='{.spec.host}'
# my-java-app-deleteme-1.apps.rhos.agriculture.gov.ie
```
Past the URL in your browser and you should be able to see your application. 



### Troubleshooting Problems 

### Logs

- If something wrong happens while deploying (like ``oc rollout latest``) you can check the logs of the container by doing: 

```sh
oc get pod | grep my-java-app
# my-java-app-1-build                 0/1       Completed   0          15m
# my-java-app-2-d6zs4                 1/1       Running     0          8m
```

We see here two container [1](#appendix-1) the one with suffix ``build`` means that this container was in charge of the building process (putting your JAR in place, configuration, etc). The one with suffix ``d6zs4`` (this is random) is the one holding your application, so if something is wrong at runtime you should look for the logs there, for example: 

```sh
oc log my-java-app-2-d6zs4
log is DEPRECATED and will be removed in a future version. Use logs instead.
Starting the Java application using /opt/run-java/run-java.sh ...
exec java -javaagent:/opt/jolokia/jolokia.jar=config=/opt/jolokia/etc/jolokia.properties -XX:+UseParallelGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MaxMetaspaceSize=100m -XX:+ExitOnOutOfMemoryError -cp . -jar /deployments/spring-boot-0.0.1-SNAPSHOT.jar
I> No access restrictor found, access to any MBean is allowed
Jolokia: Agent started with URL https://10.130.3.218:8778/jolokia/

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.2.RELEASE)
```





### Features

It includes the following things:

  - Git ignore configuration file.   
  - The Spring[ documentation](https://github.com/cesarvr/Spring-Boot/blob/master/docs/Spring%20Boot%20Reference%20Guide.pdf) in PDF, HTML version can be found [here](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/).


### Source Code

In the example every time you make a call to the root URL ```/```, it will send back a greetings message.

```java
@RestController
public class HelloController {

    @RequestMapping("/")
    public String index() {
        return "Hello World!!";
    }

}
```

## Workflow

### Working in Local

To work in local you can execute:
```sh
  mvn spring-boot:run
```
It's a quick and convenient way to work in your local machine.


[![asciicast](https://asciinema.org/a/PzYoOWwc0WUQgwJmrnXv6mck0.png)](https://asciinema.org/a/PzYoOWwc0WUQgwJmrnXv6mck0)


## Openshift

### Getting Started

If you want to practice in local you can get the [oc-client](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md).

At the moment of writing this document oc-client work best with this old version of [Docker](https://download.docker.com/mac/stable/1.13.1.15353/Docker.dmg).


### Deploying Java WAR On Openshift

To deploy our code, we can create a new application:

```
   oc new-app wildfly:10.0~https://github.com/cesarvr/Spring-Boot
```

This will create an application by running the following steps:

- **Building** This will basically clone the project, fetch all dependencies and push the image the registry.
- **Deploy** As soon as the image is registered, it will be deploy in the form of a Pod, after that will be ready to accept request.

#### Expose

After deployment finish we can check the status of our micro-service using ```oc get pods```, we should see our Pod in **Running** state.

```
NAME                  READY     STATUS      RESTARTS   AGE
spring-boot-1-build   0/1       Completed   0          12m
spring-boot-1-qp4sh   1/1       Running     0          10m
```

Then we need to know the service name ```oc get svc```, we should get something like this:
```
NAME          CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
spring-boot   172.30.76.36   <none>        8080/TCP   17m
```

Now we know the name, we expose the service this way:

```
oc expose svc spring-boot
```

Now our service has a route, we can see the details with ```oc get routes```:

```
NAME          HOST/PORT                             PATH      SERVICES      PORT       TERMINATION   WILDCARD
spring-boot   spring-boot-spring.127.0.0.1.nip.io             spring-boot   8080-tcp                 None
```

Now the best part let's make our first call.

```
curl spring-boot-spring.127.0.0.1.nip.io

Hello World!!
```

<BR>


### Using Openshift UI

The main difference using this method is that service is automatically expose.

![Openshift UI](https://github.com/cesarvr/Spring-Boot/blob/master/docs/hello.gif?raw=true)

<BR>
<BR>
  
<a name="continous"/>

## Configuring Continuous Integration

### Deploying Jenkins

Now we have an Openshift application (Build, Deploy, Service andd Router), this is very good so far, but I want to orchestrate some test automation for the code, let create a simple pipeline with Jenkins. 

First go to the Openshift console, project catalogue and click to create to deploy a new Jenkins container. 

![Openshift UI](https://github.com/cesarvr/Spring-Boot/blob/master/docs/jenkins.png?raw=true)


### Permissions 

Keep in mind that this Jenkins can only operate inside the [namespace](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/) where its deployed. 

To deploy applications in other namespaces, you need to grant ``edit, view`` roles, to the **Jenkins pod**:

```sh
oc policy add-role-to-user edit system:serviceaccount:<namespace-where-jenkins-is-deployed>:<jenkins-pod> -n <the-target-namespace>
```

### Accessing

When the deployment finish you should see something like this:

![Jenkins Deployment](https://github.com/cesarvr/Spring-Boot/blob/master/docs/jenkins-deploy.png?raw=true)

Now you just need to click the router link and it will take us to Jenkins home. 

![Jenkins Home](https://github.com/cesarvr/Spring-Boot/blob/master/docs/jenkins-home.png?raw=true)


<br><br>
### Getting Maven On Jenkins

In this example we are going to use Maven, but this instructions should be similar if you are using other package manager. The next step is to install Maven using the Global Tools:

We now need to install Maven globally for Jenkins, navigate to Manage Jenkins (Again) -> Global Tool Configuration

![Maven Configuration](https://github.com/cesarvr/Spring-Boot/blob/master/docs/MavenConfig.png?raw=true)

Then go to the Maven section and choose your Maven version, for this guide I will choose 3.5.3 and set the name to **Maven353** as we going to need it later.

Now you can use Maven in your Jenkins Pipeline Script: 

```groovy
pipeline {
  agent any

  tools {
     maven 'Maven353'   # Maven Global Alias.
  }
  ...
``` 

<br><br>
### Creating A Pipeline

We need to create a Jenkins Pipeline, easy we just need to go to the home, press the menu **new items** and choose a name for your project and check Pipeline option. 

![Jenkins Pipeline](https://github.com/cesarvr/Spring-Boot/blob/master/docs/newPipeline.png?raw=true)


<br><br>
#### Adding Some Flexibility 

One way to make our pipeline more reusable is to allow the build to accept custom parameters, is very useful if we want to clone and reuse, to activate it we need to check the box "this project is parameterized" and then we are going to create 3 parameters: 

* **GIT_URL** 
  We set here the git repository, example: https://github.com/cesarvr/Spring-Boot 

* **BUILD_CONFIG** 
  We need here the name of our builder configuration object you can check this in the Openshift Console or by doing: 
  ```oc get bc```

* **DEPLOY_CONFIG** 
  This maybe is not necessary for simple projects but if you are doing something more sophisticated it can become handy, to see list your deployment config do:
  ```oc get dc```

We should end up with something like this: 

![Env vars](https://github.com/cesarvr/Spring-Boot/blob/master/docs/EnvVars.png?raw=true)

<br><br>
### Pipeline Script  

#### Getting Started

First we need to declare the node where this pipeline will get executed, as we are going to use a standalone Jenkins local to our config project we can keep it simple.

```groovy
node { 
  // Declare stages here 

}
```

#### Preparation 

In the first stage we are going to clone the repository **GIT_CLONE** and declare a mvnHome pointing to our Maven configuration **Maven353** defined above.  

```groovy
node { 
 // Declare stages here 
 def mvnHome

 stage('Preparation') { 

     // Get some code from a GitHub repository
     git "${params.GIT_URL}"

     // Get the Maven tool.
     // ** NOTE: This 'M3' Maven tool must be configured
     // **       in the global configuration.           
     mvnHome = tool 'Maven353'
 }
}
``` 

#### Unit Test

Once we have the code we run the test task. I put a little hack at the end because if the test case fail, the process will end with a non-zero value, if this happens the build will be marked as failed and other stages will be aborted and we want to read the report which is the next stage after this one.

```groovy
 stage('Build & Test in Jenkins') {
    // Run the maven build
    sh "'${mvnHome}/bin/mvn' test || exit 0"
 }

``` 

#### Report

Everytime we run a report is generated. Here we just read the unit test report, which will help us to identify the problem. If a test fails the build is marked as unstable.  

```
 stage('Generating Unit Test Report') {
    junit '**/target/surefire-reports/TEST-*.xml'
 }
 
``` 

#### Build & Deploy

Here I check the status of the deployment if the build is unstable then I just ignore this step and finish the build. If the build state is successful state then I tell our Openshift to start a new build and deployment of our code into a container. 

```groovy
stage('Build & Deploy in Openshift') {
      /*
       * User and password should be provided by a Jenkins Credential Manager
       * Also we check the Build Status as we don't want to deploy code that is not 100% unit tested.
       *
       *  $BUILD_CONFIG, $DC_CONFIG are Jenkins project parameters we declared above. 
       *
       */
      if(currentBuild.result != 'UNSTABLE')
      sh '''
            oc login <IP-Address> --token=k3NFaYRHiTwS2KpJNBldS9.... --insecure-skip-tls-verify

            #We push our generated binaries and start an Openshift build.
            oc start-build $BUILD_CONFIG --from-dir=\'.\' -F

            #After build is finish, we now look watch the deployment.
            oc rollout status $DC_CONFIG -w

            #Bye Bye...
            oc logout'''
   }

```


This line log us in Openshift project, you can get the token using oc whoami -t , the IP address is the one of your Openshift installation. 

```
oc login 192.168.65.2:8443 --token=bMG7rvw71f_z8w... --insecure-skip-tls-verify
```

Here we tell Openshift to make a new [build](https://docs.openshift.com/enterprise/3.2/dev_guide/builds.html) and we want to push the content of this folder. This will speed up the image generation speed. 

```
oc start-build $BUILD_CONFIG --from-dir=\'.\' -F 
```

After this step finishes we just wait for the [deployment](https://docs.openshift.com/enterprise/3.0/dev_guide/deployments.html) phase to finish.

```
oc rollout status $DEPLOY_CONFIG -w 
```

Here you can find the definition for the command we are using here: 

* [oc login](https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html)
* [oc start-build](https://docs.openshift.org/latest/cli_reference/basic_cli_operations.html#start-build)
* [oc rollout](https://docs.openshift.com/container-platform/3.3/dev_guide/deployments/basic_deployment_operations.html#viewing-a-deployment)


<br><br>
#### Pipeline Script

Full Jenkins script is this [Gist](https://gist.github.com/cesarvr/fe524d24f259d8c0259f521a0a0319c3).

<a name="faster"/>

# Faster Continuos Integration

This is an alternative and faster approach, which allow you to use Openshift integration with Jenkins. It works by creating a Openshift [JenkinsPipeline/Builder](https://docs.openshift.com/container-platform/3.7/dev_guide/openshift_pipeline.html#jenkins-pipeline-strategy) which take care of setup all the necessary components. All you need is a [Jenkins script file](https://github.com/cesarvr/Spring-Boot/blob/master/Jenkinsfile), in this case the file is provided as a part of the project.


First we create our project as described before. 

```sh
  oc new-app wildfly:10.0~https://github.com/cesarvr/Spring-Boot --name=spring-boot
```

Now we need to create our BuilderConfig with strategy type JenkinsPipeline.

```sh
  oc new-build wildfly:10.0~https://github.com/cesarvr/Spring-Boot --name=spring-app --strategy=pipeline
```

A BuilderConfig object named spring-app is created, we pass the Git URL with the Jenkins Pipeline script definition.

If you remember, our Jenkins pipeline script accepts a parameter to target our app (Builder, Deployment) using a parameter called  [PROJECT_NAME](https://github.com/cesarvr/Spring-Boot/blob/master/Jenkinsfile#L12), we can do this by writing:

```sh
  oc set env bc/spring-app PROJECT_NAME=spring-boot
```

This will inject **spring-boot** as the pre-defined value. 

Next we can start our pipeline:

```sh
  oc start-build bc/spring-app
```

After we complete above step, Openshift will perform the following steps: 

- Jenkins instance. The step is skipped if it was already created.
- Add a new pipeline project and includes the above pipeline within this project. 

![Openshift UI](https://github.com/cesarvr/Spring-Boot/blob/master/docs/pipeline.png?raw=true)

- Integrates with Openshift Console, this means that you can check the pipeline status from the dashboard. 

And thats it, you just need to setup your Webhooks and start working in your app.

Thanks to [martineg](https://github.com/martineg) and [Prima](https://github.com/primashah), for the help with this one. 






## Appendix

<a name="appendix-1"/>
In reality Openshift uses an abstraction called **pod** whose purpose is to facilitate the deployment of one or many containers and made them behave as a single entity (or a single container). [For more information about pods](https://kubernetes.io/docs/concepts/workloads/pods/pod/)
