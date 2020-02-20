package hello;

import brave.sampler.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class HelloController {
    @Autowired
    private Environment env;
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @RequestMapping("/")
    public String index() {
        logger.info("{}", env.getProperty("ENDPOINT"));
        return "Greetings from Spring Boot!";
    }

    @Bean
    public Sampler alwaysSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }

    @RequestMapping("/pong")
    public String pong() {
        return "Pong!";
    }

    @RequestMapping("/ping")
    public String ping() {

        String endpoint = env.getProperty("PONG_ENDPOINT");

        if(endpoint == null || endpoint.isEmpty()){
            logger.error("You need to specify an Endpoint environment variable in the container.");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "service unavailable."
            );
        }else{
            logger.info("Calling our endpoint -> " + endpoint);
            String response = (String) restTemplate.exchange(endpoint,
                    HttpMethod.GET, null, new ParameterizedTypeReference<String>() {}).getBody();

            return "Ping! " + response;
        }
    }

}
