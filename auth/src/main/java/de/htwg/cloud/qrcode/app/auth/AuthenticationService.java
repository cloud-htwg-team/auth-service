package de.htwg.cloud.qrcode.app.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AuthenticationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(FAIL_ON_UNKNOWN_PROPERTIES);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String googleCloudIdentityProviderApiKey;

    public AuthenticationService(
            @Value("${google.cloud.identity-platform.api-key}") String apiKey
    ) {
        googleCloudIdentityProviderApiKey = apiKey;
        // log.info(googleCloudIdentityProviderApiKey);
    }

    public UserInfoDto login(LoginDto loginDto) throws URISyntaxException, IOException, InterruptedException {
        URI loginURI = new URI("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s".formatted(
                googleCloudIdentityProviderApiKey
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(loginDto);
//        log.info(json);

        HttpRequest historyServicePOSTRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(historyServicePOSTRequest, HttpResponse.BodyHandlers.ofString());
//        log.info("Response received: {}", response.body());

        if (response.statusCode() != 200) {
            return null;
        }

        LoginUserResponse responseDto = OBJECT_MAPPER.readValue(response.body(), LoginUserResponse.class);

        return new UserInfoDto(responseDto.localId, loginDto.tenantId(), responseDto.idToken);
    }

    public ResponseEntity<String> verify(String idToken) throws IOException, InterruptedException, URISyntaxException {
        URI loginURI = new URI("https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=%s".formatted(
                googleCloudIdentityProviderApiKey
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(new VerifyDto(idToken));
        log.info(json);

        HttpRequest historyServicePOSTRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(historyServicePOSTRequest, HttpResponse.BodyHandlers.ofString());
        log.info("Response received: {}", response.body());

        if (response.statusCode() == 400) {
            VerifyResponseInvalid responseInvalid = OBJECT_MAPPER.readValue(response.body(), VerifyResponseInvalid.class);
            return ResponseEntity.status(401).body("Message from GCIP: " + responseInvalid.getError().message);
        }

        if (response.statusCode() != 200) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Response status from GCIP: " + response.statusCode());
        }

        return ResponseEntity.ok().body("ID Token verified.");
    }

    public record UserInfoDto(String userId, String tenantId, String idToken) {}

    private record VerifyDto(String idToken) {}

    public record LoginDto(String email, String password, String tenantId) {}

    private record LoginUserResponse(String localId, String idToken) {}

    @Data
    private static class VerifyResponseInvalid {
        @JsonProperty
        private Error error;

        private static class Error {
            @JsonProperty
            private String message;
        }
    }

}
