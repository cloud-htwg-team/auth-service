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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    private final String terraformServiceServer;
    private final String terraformServicePort;

    public AuthenticationService(
            @Value("${google.cloud.identity-platform.api-key}") String apiKey,
            @Value("${terraform.service.server}")  String terraformServiceServer,
            @Value("${terraform.service.port}")  String terraformServicePort) {
        this.googleCloudIdentityProviderApiKey = apiKey;
        this.terraformServiceServer = terraformServiceServer;
        this.terraformServicePort = terraformServicePort;
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
                .timeout(Duration.ofSeconds(180))
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
                .timeout(Duration.ofSeconds(180))
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
                .timeout(Duration.ofSeconds(180))
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
    public boolean runTerraformApply(String tenant) {
        URI terraformServiceURI = new URI("http://%s:%s/secure/apply".formatted(
                terraformServiceServer,
                terraformServicePort
        ));

        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(new TerraformApplyDto(tenant));
//        log.info(json);

        HttpRequest applyRequest = HttpRequest.newBuilder()
                .uri(terraformServiceURI)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(600))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Calling terraform service...");
        HttpResponse<String> response = HTTP_CLIENT.send(applyRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Tenant service response status: {}", response.statusCode());
            log.warn(response.body());
            return false;
        }

        log.info("Terraform applied.");
        return true;
    }


    public record UserInfoDto(String userId, String tenantId, String idToken) {}

    private record VerifyDto(String idToken) {}

    // sign up + login operations
    public record UserSignDto(String email, String password, String tenantId) {}

    private record LoginUserResponse(String localId, String idToken) {}

    private record TerraformApplyDto(String tenantName) {}

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
