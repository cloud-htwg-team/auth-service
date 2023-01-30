package de.htwg.cloud.qrcode.app.auth;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.multitenancy.Tenant;
import de.htwg.cloud.qrcode.app.auth.AuthenticationService.UserInfoDto;
import de.htwg.cloud.qrcode.app.auth.AuthenticationService.UserSignDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Slf4j
@RestController
@RequestMapping
public class AuthenticationApi {
    private final String customAuthHeaderName;
    private final AuthenticationService service;

    public AuthenticationApi(
            @Value("${auth.header}") String customAuthHeaderName,
            AuthenticationService service
    ) {
        this.customAuthHeaderName = customAuthHeaderName;
        this.service = service;
    }


    @PostMapping(path = "/create-tenant", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Tenant> createTenant(@RequestBody CreateTenantDto dto) throws FirebaseAuthException {
        log.info("Endpoint: /create-tenant. Name: " + dto.name());
        if (dto.name() == null || dto.name().isBlank()) {
            log.info("Bad Data received: {}", dto.name());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Tenant tenant = service.createTenant(dto.name());

        boolean success = service.runTerraformApply(dto.name());
        if (!success) {
            throw new RuntimeException("Terraform apply has failed");
        }

        return ResponseEntity.ok().body(tenant);
    }

    @DeleteMapping(path = "/delete-tenant")
    public void deleteTenant(@RequestParam(name = "tenantId") String tenantId) throws FirebaseAuthException {
        log.info("Endpoint: /delete-tenant. tenantId: " + tenantId);
        service.deleteTenant(tenantId);
    }

    @PostMapping(path = "/sign-up", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> signUp(@RequestBody UserSignDto dto) throws URISyntaxException, IOException, InterruptedException {
        log.info("Endpoint: /sing-up. Email: " + dto.email());
        if (dto.email() == null || dto.email().isBlank()
            || dto.password() == null || dto.password().isBlank()
            || dto.tenantId() == null || dto.tenantId().isBlank()) {
            log.info("Bad Data received: {} {} {}", dto.email(), dto.password(), dto.tenantId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        return service.signUp(dto);
    }

    @PostMapping(path = "/login", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfoDto> login(@RequestBody UserSignDto dto) throws URISyntaxException, IOException, InterruptedException {
        log.info("Endpoint: /login. Email: " + dto.email());
        if (dto.email() == null || dto.email().isBlank()
            || dto.password() == null || dto.password().isBlank()
            || dto.tenantId() == null || dto.tenantId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        UserInfoDto user = service.login(dto);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(user);

    }

    @PostMapping(path = "/verify", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(@RequestHeader HttpHeaders headers) throws IOException, URISyntaxException, InterruptedException {
        List<String> headerValues = headers.get(customAuthHeaderName);
        if (headerValues == null || headerValues.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Required header is missing: " + customAuthHeaderName);
        }

        log.info(headers.toString());
        String idToken = headerValues.get(0);

        log.info("Endpoint: /verify. IdToken (shortened): " + idToken.substring(0, 20) + "..." + idToken.substring(idToken.length() - 20) + "  Length: " + idToken.length());

        return service.verify(idToken);
    }


    @GetMapping(path = "/list-tenants", produces = APPLICATION_JSON_VALUE)
    public List<Tenant> tenants() throws FirebaseAuthException {
        log.info("Endpoint: /list-tenants");
        return service.listTenants();
    }

    private record CreateTenantDto(String name) {}
}
