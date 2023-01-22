package de.htwg.cloud.qrcode.app.auth;

import de.htwg.cloud.qrcode.app.auth.AuthenticationService.LoginDto;
import de.htwg.cloud.qrcode.app.auth.AuthenticationService.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping
public class AuthenticationApi {
    private static final String AUTH_HEADER_ID_TOKEN = "USER_ID_TOKEN";

    private final AuthenticationService service;

    public AuthenticationApi(AuthenticationService service) {
        this.service = service;
    }


    @PostMapping(path = "/login", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfoDto> login(@RequestBody LoginDto dto) throws URISyntaxException, IOException, InterruptedException {
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

    @PostMapping(path = "/verify")
    public ResponseEntity<String> verify(@RequestHeader(name = AUTH_HEADER_ID_TOKEN) String idToken) throws IOException, URISyntaxException, InterruptedException {
        boolean verified = service.verify(idToken);

        if (!verified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        return ResponseEntity
                .ok()
                .body("ID Token verified.");
    }

}
