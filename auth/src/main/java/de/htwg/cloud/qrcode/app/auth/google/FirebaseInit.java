package de.htwg.cloud.qrcode.app.auth.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FirebaseInit {

    private final String firebaseSdkKeyProperty;
    private final String firebaseSdkKeyJson;

    public FirebaseInit(
            @Value("${firebase.admin.sdk.key}") String firebaseSdkKeyProperty,
            @Value("${firebase.admin.sdk.json}") String firebaseSdkKeyJsonLocation) {
        this.firebaseSdkKeyProperty = firebaseSdkKeyProperty;
        this.firebaseSdkKeyJson = firebaseSdkKeyJsonLocation;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("SDK Key is prepared: {}", firebaseSdkKeyProperty);

        InputStream serviceAccount;
        if (firebaseSdkKeyProperty == null || firebaseSdkKeyProperty.isBlank()) {
            // use file
            log.info("Attempting to use file at: {}", firebaseSdkKeyJson);
            serviceAccount = new ClassPathResource(firebaseSdkKeyJson).getInputStream();
        } else {
            // use property
            log.info("Attempting to use property: {}", firebaseSdkKeyProperty);
            serviceAccount = new ByteArrayInputStream(firebaseSdkKeyProperty.getBytes(StandardCharsets.UTF_8));
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        log.info("SDK Initialized with {}", credentials);
    }

}
