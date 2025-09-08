package com.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class GoogleDocReader {
    private static final String APP_NAME = "My Docs App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/documents.readonly");
    private static final String CLIENT_SECRET_PATH = "config/client_secret.json";

    private static com.google.api.client.auth.oauth2.Credential authorize(NetHttpTransport http) throws Exception {
        try (InputStream in = java.nio.file.Files.newInputStream(Path.of(CLIENT_SECRET_PATH))) {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            var flow = new GoogleAuthorizationCodeFlow.Builder(http, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(Path.of("tokens").toFile()))
                    .setAccessType("offline")
                    .build();
            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver.Builder().setPort(0).build())
                    .authorize("user");
        }
    }

    public static void main(String[] args) throws Exception {
        String docId = (args.length > 0)
                ? args[0]
                : "1Z9gQnt7wiGHDmlvWjYqW1klEBHyS7Sn2oGVLNoXFzWI";

        var http = GoogleNetHttpTransport.newTrustedTransport();
        var cred = authorize(http);
        var docs = new Docs.Builder(http, JSON_FACTORY, cred).setApplicationName(APP_NAME).build();

        Document doc = docs.documents().get(docId).execute();
        System.out.println("Title: " + doc.getTitle());
        System.out.println("-----");
        String text = extractText(doc);
        System.out.println(text);
    }

    /** High-level extractor that pulls visible text from body, tables, and TOC. */
    private static String extractText(Document doc) {
        StringBuilder out = new StringBuilder();
        var body = doc.getBody();
        if (body != null && body.getContent() != null) {
            readStructuralElements(body.getContent(), out);
        }
        return out.toString();
    }

    /** Walk the document structure recursively. */
    private static void readStructuralElements(List<StructuralElement> elements, StringBuilder out) {
        if (elements == null) return;
        for (StructuralElement el : elements) {
            if (el.getParagraph() != null) {
                readParagraph(el.getParagraph(), out);
            } else if (el.getTable() != null) {
                readTable(el.getTable(), out);
            } else if (el.getTableOfContents() != null) {
                readStructuralElements(el.getTableOfContents().getContent(), out);
            }
        }
    }

    private static void readParagraph(Paragraph p, StringBuilder out) {
        if (p.getElements() == null) return;
        for (ParagraphElement pe : p.getElements()) {
            TextRun tr = pe.getTextRun();
            if (tr != null && tr.getContent() != null) {
                out.append(tr.getContent());
            }
        }
    }

    private static void readTable(Table t, StringBuilder out) {
        if (t.getTableRows() == null) return;
        for (TableRow row : t.getTableRows()) {
            if (row.getTableCells() == null) continue;
            for (TableCell cell : row.getTableCells()) {
                readStructuralElements(cell.getContent(), out);
            }
        }
    }
}