# Spring Boot in Openshift

This example project is for people that want to start playing with Spring Boot in Openshift.

Its based in the hello world located in the [Spring website](https://spring.io/guides/gs/spring-boot/), I just modify the ```pom.xml``` so the code can be deploy using the Wildfly/Openshift template.

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

### Working in local

To work in local you can execute:
```sh
  mvn spring-boot:run
```
It's a quick and convenient way to work in your local machine.


[![asciicast](https://asciinema.org/a/PzYoOWwc0WUQgwJmrnXv6mck0.png)](https://asciinema.org/a/PzYoOWwc0WUQgwJmrnXv6mck0)


## Openshift

### Trying Openshift

If you want to practice in local you can get the [oc-client](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md).

At the moment of writing this document oc-client work best with this old version of [Docker](https://download.docker.com/mac/stable/1.13.1.15353/Docker.dmg).


### Deploying in Openshift

To deploy our code, we can create a new application:

```
oc new-app wildfly:10.0~https://github.com/cesarvr/Spring-Boot
```

This will create an application by running following steps:

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


##### Live Demo

[![asciicast](https://asciinema.org/a/vYisX98uO9b9xr0XKWwUyW2SI.png)](https://asciinema.org/a/vYisX98uO9b9xr0XKWwUyW2SI)



##### Using Openshift UI

The main difference using this method is that service is automatically expose.

![Openshift UI](https://github.com/cesarvr/Spring-Boot/blob/master/docs/hello.gif?raw=true)

<BR>
<BR>

## Configuring CI

Now we have an Openshift application (Build, Deploy, Expose), this is very good so far, but I want to orchestrate some test automation for the code, let create a simple pipeline with Jenkins. 

First go to the Openshift console, project catalago and click to create a new Jenkins application, the advantage by doing this is this application get the permission to operate this project. 

![Openshift UI](https://github.com/cesarvr/Spring-Boot/blob/master/docs/jenkins.png?raw=true)



### Maven

In this example project we are using Maven, but instructions should be similar if you are using other package manager. Now we need to go to: 

* Manage Jenkins -> Manage Plugins. 

![Manage Plugins](https://github.com/cesarvr/Spring-Boot/blob/master/docs/Manage.png?raw=true)


* Install Pipeline Maven Integration Jenkins Plugin, Then just press the button install without restart.

![Maven Integration Plugin](https://github.com/cesarvr/Spring-Boot/blob/master/docs/Maven%20Plugin.png?raw=true)

* We now need to install Maven globally for Jenkins, navigate to Manage Jenkins (Again) -> Global Tool Configuration

![Maven Configuration](https://github.com/cesarvr/Spring-Boot/blob/master/docs/MavenConfig.png?raw=true)

* Then go to the Maven section and choose your Maven version, for this guide I will choose 3.5.3 and set the name to Maven353 as we going to need it later.

* Then press save. We finish the boring part, let’s create our pipeline. 


### Building a Pipeline

Next we need to go to Jenkin’s home, press the menu **new items** and choose a name for your project and check Pipeline option. 

![Jenkins Pipeline](https://github.com/cesarvr/Spring-Boot/blob/master/docs/newPipeline.png?raw=true)


```groovy
node {
    stage('Preparation') { // for display purposes
    // Get some code from a GitHub repository
    sh "rm -rf target"
    git "${params.GIT_URL}"
    // Get the Maven tool.
    // ** NOTE: This 'M3' Maven tool must be configured
    // **       in the global configuration.           
    mvnHome = tool 'Maven353'
 }

 stage('Build') {
    // Run the maven build
    sh "'${mvnHome}/bin/mvn' test || exit 0"
 }



 stage('Publish') {
      //user and password should be provided by a Jenkins Credential Manager
      sh '''oc login 192.168.65.2:8443 --token=k3NFaYRHiTwS2KpJNBldS9p7XVIJrofR5PPf6K7FmVs --insecure-skip-tls-verify

            #We push our generated binaries and start an Openshift build.
            oc start-build $BUILD_CONFIG --from-dir=\'.\' -F

            #After build is finish, we now look watch the deployment.
            oc rollout status $DC_CONFIG -w

            #Bye Bye...
            oc logout'''
   }

   stage('Integration') {
    // Put here some external validation call to your Pod    
   }

   stage('Generating Report') {
    junit '**/target/surefire-reports/TEST-*.xml'
   }


}

```
