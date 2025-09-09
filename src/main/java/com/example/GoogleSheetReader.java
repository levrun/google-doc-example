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

        String sheetName = null;
        for (Sheet sheet : sheets) {
            if ("25".equals(sheet.getProperties().getTitle())) {
                sheetName = sheet.getProperties().getTitle();
                break;
            }
        }

        if (sheetName == null) {
            System.out.println("Sheet with name \"25\" does not exist.");
            return;
        }

        System.out.println("Reading from sheet: " + sheetName);

        // Rows in Sheets API are 1-based, so rows 41 and 76 are "41:41" and "76:76"
        String range41 = sheetName + "!41:41";
        String range76 = sheetName + "!76:76";

        ValueRange row41 = service.spreadsheets().values().get(spreadsheetId, range41).execute();
        ValueRange row76 = service.spreadsheets().values().get(spreadsheetId, range76).execute();

        System.out.println("Row 41:");
        if (row41.getValues() != null && !row41.getValues().isEmpty()) {
            System.out.println(row41.getValues().get(0));
        } else {
            System.out.println("No data found in row 41.");
        }

        System.out.println("Row 76:");
        if (row76.getValues() != null && !row76.getValues().isEmpty()) {
            List<Object> row76Values = row76.getValues().get(0);
            System.out.println(row76Values);

            // Fetch cell notes for row 76 using the "fields" parameter
            // This requires an additional request for cell metadata
            GridRange gridRange = new GridRange()
                    .setSheetId(null) // We'll find the correct sheetId below
                    .setStartRowIndex(75) // 0-based, so 76th row is index 75
                    .setEndRowIndex(76)
                    .setStartColumnIndex(0);

            // Find the sheetId for sheet "25"
            Integer sheetId = null;
            for (Sheet sheet : sheets) {
                if ("25".equals(sheet.getProperties().getTitle())) {
                    sheetId = sheet.getProperties().getSheetId();
                    break;
                }
            }
            if (sheetId != null) {
                gridRange.setSheetId(sheetId);

                // Request cell metadata (notes)
                Spreadsheet sheetMeta = service.spreadsheets()
                        .get(spreadsheetId)
                        .setRanges(Collections.singletonList(sheetName + "!76:76"))
                        .setFields("sheets.data.rowData.values.note")
                        .execute();

                List<Sheet> metaSheets = sheetMeta.getSheets();
                if (metaSheets != null && !metaSheets.isEmpty()) {
                    List<RowData> rowData = metaSheets.get(0).getData().get(0).getRowData();
                    if (rowData != null && !rowData.isEmpty()) {
                        List<CellData> cellDataList = rowData.get(0).getValues();
                        for (int i = 0; i < cellDataList.size(); i++) {
                            CellData cell = cellDataList.get(i);
                            String note = cell.getNote();
                            if (note != null && !note.isEmpty()) {
                                System.out.println("Note in column " + (i + 1) + ": " + note);
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("No data found in row 76.");
        }
    }
}