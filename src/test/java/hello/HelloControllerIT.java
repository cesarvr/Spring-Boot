package hello;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloControllerIT {

	@LocalServerPort
	private int port;

	private URL base;

	@Autowired
	private TestRestTemplate template;

    @BeforeEach
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/");
    }

    @Test
    public void getHello() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString(),
                String.class);
        assertThat(response.getBody().equals("Greetings from Spring Boot!"));
    }

    @Test
    public void ping() throws Exception {
        System.setProperty("PONG_ENDPOINT", "http://localhost:"+ port + "/pong");

        String endpoint = base.toString() + "ping";
        System.out.println("Testing ping endpoint: " + endpoint);
        ResponseEntity<String> response = template.getForEntity(endpoint,
                String.class);

        System.out.println("response: " + response.getBody());
        assertThat(response.getBody().equals("Ping! Pong!"));

        //System.setProperty("PONG_ENDPOINT", null);
    }
}
