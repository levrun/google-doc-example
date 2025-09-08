package com.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class GoogleDocWriter {
    private static final String APP_NAME = "My Docs App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/documents"
    );
    private static final String CLIENT_SECRET_PATH = "config/client_secret.json";

    private static com.google.api.client.auth.oauth2.Credential authorize(NetHttpTransport http) throws Exception {
        System.out.println("[auth] Loading " + CLIENT_SECRET_PATH + " from file system...");
        try (InputStream in = java.nio.file.Files.newInputStream(Path.of(CLIENT_SECRET_PATH))) {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            var tokenDir = Path.of("tokens").toFile();
            System.out.println("[auth] Token store: " + tokenDir.getAbsolutePath());

            var flow = new GoogleAuthorizationCodeFlow.Builder(http, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(tokenDir))
                    .setAccessType("offline")
                    .build();

            var receiver = new LocalServerReceiver.Builder().setPort(0).build(); // pick a free port
            System.out.println("[auth] Starting local OAuth receiver on a random free port...");
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[main] Starting " + APP_NAME);
        var http = GoogleNetHttpTransport.newTrustedTransport();
        var cred = authorize(http);

        // 1) Create Doc via Drive API
        var drive = new Drive.Builder(http, JSON_FACTORY, cred).setApplicationName(APP_NAME).build();
        var meta = new File().setName("My New Document").setMimeType("application/vnd.google-apps.document");
        System.out.println("[drive] Creating Google Doc file...");
        var created = drive.files().create(meta).setFields("id").execute();
        var docId = created.getId();
        System.out.println("[drive] Created: https://docs.google.com/document/d/" + docId + "/edit");

        // 2) Write text via Docs API
        var docs = new Docs.Builder(http, JSON_FACTORY, cred).setApplicationName(APP_NAME).build();
        var requests = List.of(
                new Request().setInsertText(new InsertTextRequest()
                        .setText("Hello, world! This is my first Google Doc created with Java.\n")
                        .setLocation(new Location().setIndex(1)))
        );
        System.out.println("[docs] Inserting text...");
        docs.documents().batchUpdate(docId, new BatchUpdateDocumentRequest().setRequests(requests)).execute();
        System.out.println("[done] Text inserted.");
    }
}