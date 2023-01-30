package de.htwg.cloud.qrcode.app.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.multitenancy.ListTenantsPage;
import com.google.firebase.auth.multitenancy.Tenant;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public Tenant createTenant(String tenantName) throws FirebaseAuthException {
        Tenant.CreateRequest request = new Tenant.CreateRequest()
                .setDisplayName(tenantName)
                .setEmailLinkSignInEnabled(true)
                .setPasswordSignInAllowed(true);

        Tenant tenant = FirebaseAuth.getInstance().getTenantManager().createTenant(request);
        System.out.println("Created tenant: " + tenant.getTenantId());

        return tenant;
    }

    public ResponseEntity<String> signUp(UserSignDto dto) throws IOException, InterruptedException, URISyntaxException {
        URI loginURI = new URI("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=%s".formatted(
                googleCloudIdentityProviderApiKey
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
//        log.info(json);

        HttpRequest singUpRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(singUpRequest, HttpResponse.BodyHandlers.ofString());
        log.info("Response received: {}", response.body());

        if (response.statusCode() == 400) {
            VerifyResponseInvalid responseInvalid = OBJECT_MAPPER.readValue(response.body(), VerifyResponseInvalid.class);
            return ResponseEntity.status(401).body("Message from GCIP: " + responseInvalid.getError().message);
        }

        if (response.statusCode() != 200) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Response status from GCIP: " + response.statusCode());
        }

        LoginUserResponse responseDto = OBJECT_MAPPER.readValue(response.body(), LoginUserResponse.class);

        var user = new UserInfoDto(responseDto.localId, dto.tenantId(), responseDto.idToken);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(user));
    }

    public UserInfoDto login(UserSignDto dto) throws URISyntaxException, IOException, InterruptedException {
        URI loginURI = new URI("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s".formatted(
                googleCloudIdentityProviderApiKey
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
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

        return new UserInfoDto(responseDto.localId, dto.tenantId(), responseDto.idToken);
    }

    public ResponseEntity<String> verify(String idToken) throws IOException, InterruptedException, URISyntaxException {
        URI loginURI = new URI("https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=%s".formatted(
                googleCloudIdentityProviderApiKey
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(new VerifyDto(idToken));
//        log.info(json);

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(loginRequest, HttpResponse.BodyHandlers.ofString());
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

    public List<Tenant> listTenants() throws FirebaseAuthException {
        ListTenantsPage page = FirebaseAuth.getInstance().getTenantManager().listTenants(null);
        List<Tenant> allTenants = new ArrayList<>();

        for (Tenant tenant : page.iterateAll()) {
            allTenants.add(tenant);
        }

        return allTenants;
    }

    public void deleteTenant(String tenantId) throws FirebaseAuthException {
        FirebaseAuth.getInstance().getTenantManager().deleteTenant(tenantId);
    }

    @SneakyThrows
    public void runTerraform(String tenant) {
        log.info("Starting process...");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "/terraform apply -auto-approve -var=\"namespace=%s\"".formatted(tenant));
        processBuilder.directory(new File("/opt/terraform/tenant"));
        //Sets the source and destination for subprocess standard I/O to be the same as those of the current Java process.
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        log.info("Waiting for end...");
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            String result = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));
            log.warn(result);
            throw new RuntimeException("execution of script failed!");
        }
        log.info("Done.");
    }


    public record UserInfoDto(String userId, String tenantId, String idToken) {}

    private record VerifyDto(String idToken) {}

    // sign up + login operations
    public record UserSignDto(String email, String password, String tenantId) {}

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
