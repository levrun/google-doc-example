package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class GoogleSheetReader {
    private static final String APPLICATION_NAME = "Google Sheets Reader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "config/client_secret.json";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

    private static com.google.api.client.auth.oauth2.Credential authorize() throws Exception {
        InputStream in = java.nio.file.Files.newInputStream(Path.of(CREDENTIALS_FILE_PATH));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(Path.of(TOKENS_DIRECTORY_PATH).toFile()))
                .setAccessType("offline")
                .build();
        var receiver = new com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder().setPort(8888).build();
        return new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java GoogleSheetReader <spreadsheetId>");
            return;
        }
        final String spreadsheetId = args[0];

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = authorize();

        Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Get spreadsheet metadata to list all sheets
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        List<Sheet> sheets = spreadsheet.getSheets();

        if (sheets == null || sheets.isEmpty()) {
            System.out.println("No sheets found.");
            return;
        }

        System.out.println("Sheet names:");
        for (Sheet sheet : sheets) {
            String sheetName = sheet.getProperties().getTitle();
            System.out.println(" - " + sheetName);
        }
    }
}