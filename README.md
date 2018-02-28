# Spring Boot in Openshift

This example project is for people that want to start playing with Spring Boot in Openshift.

Its based in the hello world located in the [Spring website](https://spring.io/guides/gs/spring-boot/), I just modify the ```pom.xml``` so the code can be deploy using the Wildfly/Openshift template.

### Features

It includes the following things:

  - Git ignore configuration file.   
  - The Spring[ documentation](https://github.com/cesarvr/Spring-Boot/blob/master/docs/Spring%20Boot%20Reference%20Guide.pdf) in PDF, HTML version can be foundg [here](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/).


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


### Deploying in Openshift

To deploy our code, we can create a new application:

```
oc new-app wildfly:10.0~https://github.com/cesarvr/Spring-Boot
```

This will take the project through various steps:

- **Building** This will basically clone the project, fetch all dependencies and push the image the registry.
- **Deploy** As soon as the image is registered, it will be deploy in the form of a Pod, after that will be accessible.

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





