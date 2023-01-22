package de.htwg.cloud.qrcode.app.auth;

import de.htwg.cloud.qrcode.app.auth.AuthenticationService.LoginDto;
import de.htwg.cloud.qrcode.app.auth.AuthenticationService.UserInfoDto;
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

        log.info(idToken);

        return service.verify(idToken);
    }

}
