package dasniko.smtp;

import io.restassured.RestAssured;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Testcontainers
public class SmtpTest {

    private static final Integer PORT_SMTP = 1025;
    private static final Integer PORT_HTTP = 8025;

    Integer smtpPort;
    String smtpHost;

    @Container
    public static GenericContainer<?> mailhog = new GenericContainer<>("mailhog/mailhog")
        .withExposedPorts(PORT_SMTP, PORT_HTTP)
        .waitingFor(Wait.forHttp("/"));

    @BeforeEach
    public void setUp() {
        mailhog.start();
        
        smtpPort = mailhog.getMappedPort(PORT_SMTP);
        smtpHost = mailhog.getContainerIpAddress();
        Integer httpPort = mailhog.getMappedPort(PORT_HTTP);

        RestAssured.baseURI = "http://" + smtpHost;
        RestAssured.port = httpPort;
        RestAssured.basePath = "/api/v2";
    }

    @Test
    public void testSmtp() throws Exception {
        given().when().get("/messages")
            .then().body("total", equalTo(0));

        Email email = new SimpleEmail();
        email.setHostName(smtpHost);
        email.setSmtpPort(smtpPort);
        email.setAuthenticator(new DefaultAuthenticator("john.doe", "s3cr3t"));
        email.setFrom("john.doe@testcontainer");
        email.setSubject("Testmail");
        email.setMsg("This is a test email...");
        email.addTo("test@testcontainer");
        email.send();

        given().when().get("/messages")
            .then().body("total", equalTo(1));
    }

}
