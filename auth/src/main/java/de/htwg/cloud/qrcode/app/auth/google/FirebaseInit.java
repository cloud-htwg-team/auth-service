package de.htwg.cloud.qrcode.app.auth.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FirebaseInit {

    private final String firebaseSdkKey;

    public FirebaseInit(@Value("${firebase.sdk.key}") String firebaseSdkKey) {
        this.firebaseSdkKey = firebaseSdkKey;
    }

    @PostConstruct
    public void init() throws IOException {
        InputStream serviceAccount = new ByteArrayInputStream(firebaseSdkKey.getBytes(StandardCharsets.UTF_8));

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        log.info("SDK Initialized with {}", credentials);
    }

}
