package com.example.onlinebankingapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.type}")
    private String type;

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.private-key-id}")
    private String privateKeyId;

    @Value("${firebase.private-key}")
    private String privateKey;

    @Value("${firebase.client-email}")
    private String clientEmail;

    @Value("${firebase.client-id}")
    private String clientId;

    @Value("${firebase.auth-uri}")
    private String authUri;

    @Value("${firebase.token-uri}")
    private String tokenUri;

    @Value("${firebase.auth-provider-x509-cert-url}")
    private String authProviderCertUrl;

    @Value("${firebase.client-x509-cert-url}")
    private String clientCertUrl;

    @Value("${firebase.universe-domain}")
    private String universeDomain;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        // Create a map of the values to reconstruct the JSON
        Map<String, Object> firebaseConfig = new HashMap<>();
        firebaseConfig.put("type", type);
        firebaseConfig.put("project_id", projectId);
        firebaseConfig.put("private_key_id", privateKeyId);
        firebaseConfig.put("private_key", privateKey.replace("\\n", "\n"));
        firebaseConfig.put("client_email", clientEmail);
        firebaseConfig.put("client_id", clientId);
        firebaseConfig.put("auth_uri", authUri);
        firebaseConfig.put("token_uri", tokenUri);
        firebaseConfig.put("auth_provider_x509_cert_url", authProviderCertUrl);
        firebaseConfig.put("client_x509_cert_url", clientCertUrl);
        firebaseConfig.put("universe_domain", universeDomain);

        // Convert map to JSON and then to InputStream
        ByteArrayInputStream serviceAccount = new ByteArrayInputStream(
                new ObjectMapper().writeValueAsBytes(firebaseConfig)
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}
